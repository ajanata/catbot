package com.ajanata.catbot.irc;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.Bot;
import com.ajanata.catbot.CatBot;
import com.ajanata.catbot.handlers.Handler;
import com.diffplug.common.base.Errors;


public class IrcBot extends ListenerAdapter implements Bot {

  private static final Logger LOG = LoggerFactory.getLogger(IrcBot.class);

  private static final String PROP_SERVER = "server";
  private static final String PROP_PORT = "port";
  //  private static final String PROP_TLS_ENABLED = "tls.enabled";
  private static final String PROP_REALNAME = "realname";
  private static final String PROP_CHANNELS = "channels";
  private static final String PROP_USER_MODES = "user.modes";
  private static final String PROP_TRIGGER_PREFIX = "trigger.prefix";

  private final CatBot catbot;
  private final int botId;

  private Set<String> channels;
  private PircBotX irc;
  private Thread ircThread;

  public IrcBot(final CatBot catbot, final int botId) {
    this.catbot = catbot;
    this.botId = botId;
  }

  @Override
  public Logger logger() {
    return LOG;
  }

  @Override
  public boolean login() {
    LOG.info("Logging into IRC...");
    final String server = catbot.getBotProperty(botId, PROP_SERVER);
    final int port = Integer.valueOf(catbot.getBotProperty(botId, PROP_PORT));
    //    final boolean useTls = Boolean.valueOf(catbot.getBotProperty(botId, PROP_TLS_ENABLED));
    final String nickname = catbot.getBotProperty(botId, CatBot.PROP_NICKNAME);
    final String realname = catbot.getBotProperty(botId, PROP_REALNAME);

    final int numChannels = Integer.parseInt(catbot.getBotProperty(botId, PROP_CHANNELS));
    final Set<String> chans = new HashSet<>();
    for (int i = 0; i < numChannels; i++) {
      chans.add(catbot.getBotProperty(botId, PROP_CHANNELS + "." + i));
    }
    channels = Collections.unmodifiableSet(chans);

    final Configuration config = new Configuration.Builder()
        .setName(nickname)
        .addServer(server, port)
        .setRealName(realname)
        .setLogin(nickname)
        .setVersion("CatBot")
        .setAutoReconnect(true)
        .setAutoReconnectDelay((int) TimeUnit.SECONDS.toMillis(5))
        .setAutoReconnectAttempts(100)
        .addAutoJoinChannels(channels)
        .addListener(this)
        .buildConfiguration();

    irc = new PircBotX(config);
    ircThread = new Thread(() -> {
      try {
        irc.startBot();
      } catch (IOException | IrcException e) {
        LOG.error(String.format("Unable to login to IRC at %s:%d", server, port), e);
      }
    }, "IRC thread " + botId);
    ircThread.setDaemon(true);
    ircThread.start();
    LOG.info("Started IRC thread.");
    return true;
  }

  @Override
  public void shutdown() {
    LOG.info("Shutting down IRC...");
    irc.send().quitServer();
    LOG.info("Waiting for IRC thread to terminate...");
    try {
      ircThread.join();
    } catch (final InterruptedException e) {
      // whatever
    }
  }

  @Override
  public void onConnect(final ConnectEvent event) throws Exception {
    LOG.info("Connected to " + event.getBot().getServerHostname());
    final String userModes = catbot.getBotProperty(botId, PROP_USER_MODES);
    event.getBot().send().mode(event.getBot().getNick(), userModes);
  }

  @Override
  public void onDisconnect(final DisconnectEvent event) throws Exception {
    LOG.info("Disconnected from IRC.");
  }

  @Override
  public void onMessage(final MessageEvent event) throws Exception {
    final String text = event.getMessage();
    if (text.startsWith(catbot.getBotProperty(botId, PROP_TRIGGER_PREFIX))) {
      final String fromName = event.getUser().getNick();
      LOG.trace(String.format("Message with trigger prefix from %s to %s: %s", fromName,
          event.getChannelSource(), text));

      final String[] parts = text.split("\\s+");
      if (parts.length > 0) {
        final String trigger = parts[0].substring(1);
        final String[] params = new String[parts.length - 1];
        System.arraycopy(parts, 1, params, 0, params.length);

        final Handler handler = catbot.getHandlers().get(trigger);
        if (null != handler) {
          final String channel = event.getChannel().getName();
          final String response = handler.handleCommand(botId, fromName, "", channel,
              trigger, String.join(" ", params));
          if (null != response) {
            retry(Errors.rethrow().wrap(() -> {
              event.getBot().send().message(channel, response.replace('\n', ' '));
            }));
          }
        }
      }
    }
  }
}
