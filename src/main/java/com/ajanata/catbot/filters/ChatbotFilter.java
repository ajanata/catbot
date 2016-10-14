package com.ajanata.catbot.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.CatBot;
import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;


public class ChatbotFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(ChatbotFilter.class);

  private final CatBot catbot;
  private final int filterId;
  private final ChatterBotFactory factory = new ChatterBotFactory();
  private ChatterBot bot;
  private ChatterBotSession session;

  public ChatbotFilter(final CatBot catbot, final int filterId) {
    this.catbot = catbot;
    this.filterId = filterId;
  }

  public static Filter createInstance(final CatBot catbot, final int filterId) {
    LOG.trace(String.format("createInstance(%d)", filterId));
    return new ChatbotFilter(catbot, filterId);
  }

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
  public String handleMessage(final int botId, final String fromName, final String fromId,
      final String chatId, final String message) {
    // TODO hack
    if (message.startsWith(".chatbot ")) {
      final String prompt = message.substring(".chatbot ".length());
      return think(prompt);
    } else {
      return null;
    }
  }
}
