package com.ajanata.catbot.discord;

import java.util.Properties;

import org.apache.log4j.Logger;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MentionEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

import com.ajanata.catbot.Bot;
import com.diffplug.common.base.Errors;


public class DiscordBot implements Bot {
  public static final String DISCORD_NICK = "discord.nick";
  public static final String DISCORD_TOKEN = "discord.token";

  private static final Logger LOG = Logger.getLogger(DiscordBot.class);

  private IDiscordClient client;
  private final Properties properties;

  public DiscordBot(final Properties properties) throws DiscordException {
    this.properties = properties;
  }

  @Override
  public String getShortName() {
    return "discord";
  }

  @Override
  public Properties getProperties() {
    return properties;
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
      clientBuilder.withToken(properties.getProperty(DISCORD_TOKEN));
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

    final String nick = properties.getProperty(DISCORD_NICK);
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
  public void onMentionEvent(final MentionEvent event) {
    final IMessage message = event.getMessage();
    final IGuild guild = message.getGuild();
    final IChannel channel = message.getChannel();
    final IUser author = message.getAuthor();
    final String from = author.getDisplayName(guild);
    LOG.trace(String.format("onMentionEvent from %s (%s) in %s #%s: %s", from, author.getID(),
        guild.getName(), channel.getName(), message.getContent()));

    retry(Errors.rethrow().wrap(() -> {
      message.getChannel().sendMessage("Hey why did you poke me, " + from + "?!");
    }));
  }

  private void rateLimited() {
    try {
      Thread.sleep(1000);
    } catch (final InterruptedException e) {
      // pass
    }
  }
}
