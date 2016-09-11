package com.ajanata.catbot.telegram;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import com.ajanata.catbot.Bot;
import com.diffplug.common.base.Errors;


public class TelegramBot extends TelegramLongPollingBot implements Bot {

  private static final Logger LOG = Logger.getLogger(TelegramBot.class);

  public static final String TELEGRAM_TOKEN = "telegram.token";
  public static final String TELEGRAM_NICK = "telegram.nick";
  public static final String TELEGRAM_USERNAME = "telegram.username";

  private final Properties properties;
  private final TelegramBotsApi api;

  public TelegramBot(final Properties properties) {
    this.properties = properties;
    api = new TelegramBotsApi();
  }

  @Override
  public String getShortName() {
    return "telegram";
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public String getBotUsername() {
    return getUsername();
  }

  @Override
  public String getBotToken() {
    return getToken();
  }

  @Override
  public Logger logger() {
    return LOG;
  }

  @Override
  public void onUpdateReceived(final Update update) {
    LOG.trace(String.format("onUpdateReceived(%s)", update));

    if (update.hasMessage()) {
      final Message message = update.getMessage();
      if (message.hasText()) {
        final String text = message.getText();
        if (text.contains(getNickname())) {
          final User author = message.getFrom();
          final String from = author.getUserName();

          LOG.trace(String
              .format("Poked by %s in %s: %s", from, message.getChat().getTitle(), text));

          final SendMessage send = new SendMessage();
          send.setChatId(message.getChatId().toString());
          send.setText("Hey why did you poke me, @" + from + "?!");
          send.setReplyToMessageId(message.getMessageId());

          retry(Errors.rethrow().wrap(
              () -> {
                sendMessage(send);
              }));
        }
      }
    }
  }

  @Override
  public boolean login() {
    LOG.info("Logging into Telegram...");
    try {
      api.registerBot(this);
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
}
