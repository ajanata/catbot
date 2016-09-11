package com.ajanata.catbot;

import java.util.Properties;

import sx.blah.discord.util.DiscordException;

import com.ajanata.catbot.discord.DiscordBot;
import com.ajanata.catbot.telegram.TelegramBot;


public class CatBot {
  private final DiscordBot discordBot;
  private final TelegramBot telegramBot;
  @SuppressWarnings("unused")
  private final Properties properties;

  public static void main(final String[] args) throws Exception {
    final Properties properties = new Properties();
    properties.load(CatBot.class.getResourceAsStream("/catbot.properties"));
    final CatBot app = new CatBot(properties);

    Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
      @Override
      public void run() {
        app.shutdown();
      }
    });

    app.login();

    while (true) {
      Thread.sleep(1000);
    }
  }

  public CatBot(final Properties properties) throws DiscordException {
    this.properties = properties;
    discordBot = new DiscordBot(properties);
    telegramBot = new TelegramBot(properties);
  }

  public void login() {
    int backoff = 100;
    while (!discordBot.login()) {
      try {
        Thread.sleep(backoff);
        backoff *= 2;
        if (backoff < 0) {
          backoff = Integer.MAX_VALUE;
        }
      } catch (final InterruptedException e) {
        // pass
      }
    }

    backoff = 100;
    while (!telegramBot.login()) {
      try {
        Thread.sleep(backoff);
        backoff *= 2;
        if (backoff < 0) {
          backoff = Integer.MAX_VALUE;
        }
      } catch (final InterruptedException e) {
        // pass
      }
    }
  }

  public void shutdown() {
    discordBot.shutdown();
    telegramBot.shutdown();
  }
}
