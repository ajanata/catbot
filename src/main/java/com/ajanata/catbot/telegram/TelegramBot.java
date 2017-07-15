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

package com.ajanata.catbot.telegram;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.TelegramLongPollingCommandBot;

import com.ajanata.catbot.Bot;
import com.ajanata.catbot.CatBot;
import com.ajanata.catbot.filters.Filter;
import com.ajanata.catbot.filters.Filter.FilterResult;
import com.ajanata.catbot.handlers.Handler;
import com.diffplug.common.base.Errors;


public class TelegramBot extends TelegramLongPollingCommandBot implements Bot {

  private static final Logger LOG = LoggerFactory.getLogger(TelegramBot.class);

  public static final String HACK_BOT_USERNAME_PROPERTY = "telegram.bot.username";

  private final CatBot catbot;
  private final TelegramBotsApi api;
  private final int botId;

  public TelegramBot(final CatBot catbot, final int botId) {
    this.catbot = catbot;
    this.botId = botId;
    api = new TelegramBotsApi();
  }

  @Override
  public Logger logger() {
    return LOG;
  }

  @Override
  public void processNonCommandUpdate(final Update update) {
    LOG.trace(String.format("onUpdateReceived(%s)", update));

    if (update.hasMessage()) {
      final Message message = update.getMessage();
      if (message.hasText()) {
        final String text = message.getText();
        final User author = message.getFrom();
        final String from = author.getUserName();

        // check filters
        for (final Filter filter : catbot.getFilters()) {
          final FilterResult reply = filter.filterMessage(botId, from, author.getId().toString(),
              message.getChat().getTitle(), text);
          if (null != reply) {
            final SendMessage send = new SendMessage();
            send.setChatId(message.getChatId().toString());
            send.setText(reply.message);
            if (reply.replyToPrevious) {
              send.setReplyToMessageId(message.getMessageId());
            }

            retry(Errors.rethrow().wrap(() -> {
              sendMessage(send);
            }));
            break;
          }
        }
      }
    }
  }

  @Override
  public boolean login() {
    LOG.info("Logging into Telegram...");
    try {
      api.registerBot(this);

      for (final Entry<String, Handler> entry : catbot.getHandlers().entrySet()) {
        register(new HandlerCommand(botId, entry.getKey(), entry.getValue()));
      }
      register(new HelpCommand(this));

      LOG.info("Logged into Telegram.");
      return true;
    } catch (final TelegramApiException e) {
      LOG.error("Unable to initialize Telegram.", e);
      return false;
    }
  }

  @Override
  public void shutdown() {
    LOG.info("Nothing to be done to log out of Telegram.");
  }

  @Override
  public String getBotUsername() {
    // HACK: This is called during the constructor, so we don't have catbot yet.
    if (null == catbot) {
      final String username = System.getProperty(HACK_BOT_USERNAME_PROPERTY);
      System.getProperties().remove(HACK_BOT_USERNAME_PROPERTY);
      return username;
    }
    return catbot.getBotProperty(botId, CatBot.PROP_USERNAME);
  }

  @Override
  public String getBotToken() {
    return catbot.getBotProperty(botId, CatBot.PROP_TOKEN);
  }
}
