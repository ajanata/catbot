/**
 * Copyright (c) 2016-2017, Andy Janata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import com.ajanata.catbot.filters.Filter;
import com.ajanata.catbot.filters.Filter.FilterResult;
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

  // NOTE: this does not do private messages (onPrivateMessage)
  @Override
  public void onMessage(final MessageEvent event) throws Exception {
    final String text = event.getMessage();
    final String channel = event.getChannel().getName();
    final String fromName = event.getUser().getNick();
    if (text.startsWith(catbot.getBotProperty(botId, PROP_TRIGGER_PREFIX))) {
      LOG.trace(String.format("Message with trigger prefix from %s to %s: %s", fromName,
          event.getChannelSource(), text));

      final String[] parts = text.split("\\s+");
      if (parts.length > 0) {
        final String trigger = parts[0].substring(1);
        final String[] params = new String[parts.length - 1];
        System.arraycopy(parts, 1, params, 0, params.length);

        final Handler handler = catbot.getHandlers().get(trigger);
        if (null != handler) {
          final String response = handler.handleCommand(botId, fromName, fromName, channel,
              trigger, String.join(" ", params));
          if (null != response) {
            retry(Errors.rethrow().wrap(() -> {
              event.getBot().send().message(channel, response.replace('\n', ' '));
            }));
          }
        }
      }
    } else {
      for (final Filter filter : catbot.getFilters()) {
        final FilterResult reply = filter.filterMessage(botId, fromName, fromName, channel,
            text);
        if (null != reply) {
          final String replyText;
          if (reply.replyToPrevious) {
            replyText = fromName + ": " + reply.message;
          } else {
            replyText = reply.message;
          }
          retry(Errors.rethrow().wrap(() -> {
            event.getBot().send().message(channel, replyText.replace('\n', ' '));
          }));
          break;
        }
      }
    }
  }
}
