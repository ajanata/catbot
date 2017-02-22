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
        for (final Filter filter: catbot.getFilters()) {
          final FilterResult reply = filter.handleMessage(botId, from, author.getId().toString(), message.getChat().getTitle(), text);
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

      for (Entry<String, Handler> entry : catbot.getHandlers().entrySet()) {
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
