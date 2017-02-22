package com.ajanata.catbot.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;


public class CleverbotFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(CleverbotFilter.class);

  private final ChatterBotFactory factory = new ChatterBotFactory();
  private ChatterBot bot;
  private ChatterBotSession session;

  @Override
  public void init() {
    canUse();
  }

  private boolean canUse() {
    if (null != session) {
      return true;
    }

    try {
      bot = factory.create(ChatterBotType.CLEVERBOT);
      session = bot.createSession();
      return true;
    } catch (final Exception e) {
      LOG.error("Unable to initialize Cleverbot", e);
      return false;
    }
  }

  private String think(final String thought) {
    LOG.trace(String.format("think(%s)", thought));
    return think(thought, true);
  }

  private String think(final String thought, final boolean canRetry) {
    if (canUse()) {
      try {
        return session.think(thought);
      } catch (final Exception e) {
        LOG.error("Unable to think, retry = " + canRetry, e);
        bot = null;
        session = null;
        if (canRetry) {
          return think(thought, false);
        }
      }
    }
    return null;
  }

  @Override
  public FilterResult handleMessage(final int botId, final String fromName, final String fromId,
      final String chatId, final String message) {
    // TODO hack
    if (message.startsWith(".cleverbot ")) {
      final String prompt = message.substring(".cleverbot ".length());
      return new FilterResult(think(prompt), true);
    } else {
      return null;
    }
  }
}
