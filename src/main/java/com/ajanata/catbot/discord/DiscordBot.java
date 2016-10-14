package com.ajanata.catbot.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MentionEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

import com.ajanata.catbot.Bot;
import com.ajanata.catbot.CatBot;
import com.ajanata.catbot.handlers.Handler;
import com.diffplug.common.base.Errors;


public class DiscordBot implements Bot {
  private static final String PROP_TRIGGER_PREFIX = "trigger.prefix";

  private static final Logger LOG = LoggerFactory.getLogger(DiscordBot.class);

  private IDiscordClient client;
  private final CatBot catbot;
  private final int botId;

  public DiscordBot(final CatBot catbot, final int botId) throws DiscordException {
    this.catbot = catbot;
    this.botId = botId;
  }

  @Override
  public Logger logger() {
    return LOG;
  }

  @Override
  public boolean login() {
    LOG.info("Logging into Discord...");
    try {
      final ClientBuilder clientBuilder = new ClientBuilder();
      clientBuilder.withToken(catbot.getBotProperty(botId, CatBot.PROP_TOKEN))
          .setMaxReconnectAttempts(999);
      client = clientBuilder.login();
      final EventDispatcher dispatcher = client.getDispatcher();
      dispatcher.registerListener(this);
      LOG.info("Logged into Discord!");
      return true;
    } catch (final DiscordException e) {
      LOG.error("Unable to login to Discord.", e);
      return false;
    }
  }

  @Override
  public void shutdown() {
    try {
      client.logout();
    } catch (final DiscordException | RateLimitException e) {
      LOG.error("Unable to log out", e);
    } finally {
      client = null;
    }
    LOG.info("Logged out of Discord.");
  }

  @EventSubscriber
  public void onReadyEvent(final ReadyEvent event) {
    LOG.info("Ready!");

    final String nick = catbot.getBotProperty(botId, CatBot.PROP_NICKNAME);
    for (final IGuild guild : event.getClient().getGuilds()) {
      LOG.trace(String.format("Changing nickname for server %s", guild.getName()));
      retry(Errors.rethrow().wrap(
          () -> {
            guild.setUserNickname(event.getClient().getOurUser(), nick);
          }));
      rateLimited();
    }
  }

  @EventSubscriber
  public void onMessageReceivedEvent(final MessageReceivedEvent event) {
    final IMessage message = event.getMessage();
    final String text = message.getContent();
    if (text.startsWith(catbot.getBotProperty(botId, PROP_TRIGGER_PREFIX))) {
      final IChannel channel = message.getChannel();
      final IUser author = message.getAuthor();
      final String fromName = getUserName(message);
      LOG.trace(String.format("Message with trigger prefix from %s in %s: %s", fromName,
          channel.getName(), text));

      final String[] parts = text.split("\\s+");
      if (parts.length > 0) {
        final String trigger = parts[0].substring(1);
        final String[] params = new String[parts.length - 1];
        System.arraycopy(parts, 1, params, 0, params.length);

        final Handler handler = catbot.getHandlers().get(trigger);
        if (null != handler) {
          final String response = handler.handleMessage(botId, fromName, author.getID(),
              channel.getID(), String.join(" ", params));
          if (null != response) {
            retry(Errors.rethrow().wrap(() -> {
              channel.sendMessage(response);
            }));
          }
        }
      }
    }
  }

  @EventSubscriber
  public void onMentionEvent(final MentionEvent event) {
    final IMessage message = event.getMessage();
    final IChannel channel = message.getChannel();
    final String chatName;
    if (message.getChannel().isPrivate()) {
      chatName = channel.getName();
    } else {
      final IGuild guild = message.getGuild();
      chatName = guild.getName() + " #" + channel.getName();
    }
    final IUser author = message.getAuthor();
    final String from = getUserName(message);
    LOG.trace(String.format("onMentionEvent from %s (%s) in %s: %s", from, author.getID(),
        chatName, message.getContent()));

    retry(Errors.rethrow().wrap(() -> {
      channel.sendMessage("Hey why did you poke me, " + from + "?!");
    }));
  }

  private String getUserName(final IMessage message) {
    if (message.getChannel().isPrivate()) {
      return message.getAuthor().getName();
    } else {
      return message.getAuthor().getDisplayName(message.getGuild());
    }
  }

  private void rateLimited() {
    try {
      Thread.sleep(1000);
    } catch (final InterruptedException e) {
      // pass
    }
  }
}
