package com.ajanata.catbot;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.filters.Filter;
import com.ajanata.catbot.handlers.Handler;
import com.ajanata.catbot.telegram.TelegramBot;

import sx.blah.discord.util.DiscordException;

public class CatBot {
  private static final Logger LOG = LoggerFactory.getLogger(CatBot.class);

  // config property names
  public static final String PROP_NICKNAME = "nickname";
  public static final String PROP_OWNER_ID = "owner.id";
  public static final String PROP_TOKEN = "token";
  public static final String PROP_USERNAME = "username";

  public static final String PROP_FILTERS = "filters";
  public static final String PROP_FILTER_CLASS = "class";

  public static final String PROP_HANDLERS = "handlers";
  public static final String PROP_HANDLER_TRIGGER = "trigger";
  public static final String PROP_HANDLER_CLASS = PROP_FILTER_CLASS;

  public static final String PROP_BOTS = "bots";
  public static final String PROP_BOT_CLASS = PROP_HANDLER_CLASS;

  private static final String FACTORY_METHOD_NAME = "createInstance";

  private final List<Filter> filters;
  private final Map<String, Handler> handlers;
  private final List<Bot> bots;
  private final Properties properties;

  public static void main(final String[] args) throws Exception {
    final Properties properties = new Properties();
    properties.load(CatBot.class.getResourceAsStream("/catbot.properties"));
    final CatBot catbot = new CatBot(properties);

    Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
      @Override
      public void run() {
        catbot.shutdown();
      }
    });

    catbot.login();

    while (true) {
      Thread.sleep(1000);
    }
  }

  public CatBot(final Properties properties) throws DiscordException, ClassNotFoundException {
    this.properties = properties;

    filters = Collections.unmodifiableList(loadFilters(properties));
    handlers = Collections.unmodifiableMap(loadHandlers(properties));
    bots = Collections.unmodifiableList(loadBots(properties));
    if (bots.isEmpty()) {
      throw new RuntimeException("No bots configured.");
    }
  }

  private List<Bot> loadBots(final Properties props) throws ClassNotFoundException {
    final int numBots = Integer.valueOf(props.getProperty(PROP_BOTS, "0"));
    final List<Bot> list = new ArrayList<>(numBots);
    for (int i = 0; i < numBots; i++) {
      final String className = props.getProperty(PROP_BOTS + "." + i + "." + PROP_BOT_CLASS);
      @SuppressWarnings("unchecked")
      final Class<? extends Bot> clazz = (Class<? extends Bot>) Class.forName(className);

      if (TelegramBot.class.getName().equals(className)) {
        // This dumb shit because the TelegramBot needs to be subclassed so nothing can be used from the constructor...
        System
            .setProperty(TelegramBot.HACK_BOT_USERNAME_PROPERTY, getBotProperty(i, PROP_USERNAME));
      }

      final Bot bot;
      try {
        final Constructor<? extends Bot> ctor = clazz.getConstructor(CatBot.class, int.class);
        bot = ctor.newInstance(this, i);
      } catch (final IllegalAccessException | InstantiationException | InvocationTargetException
          | NoSuchMethodException e) {
        final String msg = String.format("Unable to instantiate bot %d, class %s", i, className);
        LOG.error(msg);
        throw new RuntimeException(msg, e);
      }
      list.add(bot);
    }
    return list;
  }

  private List<Filter> loadFilters(final Properties props) throws ClassNotFoundException {
    final int numHandlers = Integer.valueOf(props.getProperty(PROP_FILTERS, "0"));
    final List<Filter> list = new ArrayList<>(numHandlers);
    for (int i = 0; i < numHandlers; i++) {
      final String className = props
          .getProperty(PROP_FILTERS + "." + i + "." + PROP_FILTER_CLASS);
      @SuppressWarnings("unchecked")
      final Class<? extends Filter> clazz = (Class<? extends Filter>) Class.forName(className);

      Filter filter = null;
      try {
        final Method factoryMethod = clazz.getMethod(FACTORY_METHOD_NAME, CatBot.class,
            int.class);
        filter = (Filter) factoryMethod.invoke(null, this, i);
      } catch (final NoSuchMethodException e) {
        // don't care, this method is optional and we'll use the default constructor instead
      } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        final String msg = String.format(
            "Unable to initialize handler %d, class %s, via %s method", i, className, FACTORY_METHOD_NAME);
        LOG.error(msg, e);
        throw new RuntimeException(msg, e);
      }

      if (null == filter) {
        try {
          filter = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
          final String msg = String.format(
              "Unable to initialize handler %d, class %s, via <init> method", i,
              className);
          LOG.error(msg, e);
          throw new RuntimeException(msg, e);
        }
      }

      list.add(filter);
    }
    return list;
  }

  private Map<String, Handler> loadHandlers(final Properties props) throws ClassNotFoundException {
    final int numHandlers = Integer.valueOf(props.getProperty(PROP_HANDLERS, "0"));
    final Map<String, Handler> map = new HashMap<>();
    for (int i = 0; i < numHandlers; i++) {
      final String trigger = props
          .getProperty(PROP_HANDLERS + "." + i + "." + PROP_HANDLER_TRIGGER);
      final String className = props
          .getProperty(PROP_HANDLERS + "." + i + "." + PROP_HANDLER_CLASS);
      @SuppressWarnings("unchecked")
      final Class<? extends Handler> clazz = (Class<? extends Handler>) Class.forName(className);

      Handler handler = null;
      try {
        final Method factoryMethod = clazz.getMethod(FACTORY_METHOD_NAME, CatBot.class,
            int.class);
        handler = (Handler) factoryMethod.invoke(null, this, i);
      } catch (final NoSuchMethodException e) {
        // don't care, this method is optional and we'll use the default constructor instead
      } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        final String msg = String.format(
            "Unable to initialize handler %d, class %s, via %s method", i, className, FACTORY_METHOD_NAME);
        LOG.error(msg, e);
        throw new RuntimeException(msg, e);
      }

      if (null == handler) {
        try {
          handler = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
          final String msg = String.format(
              "Unable to initialize handler %d, class %s, via <init> method", i,
              className);
          LOG.error(msg, e);
          throw new RuntimeException(msg, e);
        }
      }

      map.put(trigger, handler);
    }
    return map;
  }

  public void login() {
    LOG.info("Creating bots");
    for (final Bot bot : bots) {
      int backoff = 100;
      while (!bot.login()) {
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

    // FIXME do this before connecting the bots?
    LOG.info("Initializing filters");
    for (final Filter filter : filters) {
      filter.init();
    }

    LOG.info("Initializing handlers");
    for (final Handler handler : handlers.values()) {
      handler.init();
    }
  }

  public void shutdown() {
    LOG.info("Shutting down.");
    for (final Bot bot : bots) {
      bot.shutdown();
    }
  }

  public String getProperty(final String key) {
    return properties.getProperty(key);
  }

  public String getHandlerProperty(final int handlerId, final String key) {
    return properties.getProperty(PROP_HANDLERS + "." + handlerId + "." + key);
  }

  public String getFilterProperty(final int filterId, final String key) {
    return properties.getProperty(PROP_FILTERS + "." + filterId + "." + key);
  }

  public String getBotProperty(final int botId, final String key) {
    return properties.getProperty(PROP_BOTS + "." + botId + "." + key);
  }

  public Map<String, Handler> getHandlers() {
    return handlers;
  }

  public List<Filter> getFilters() {
    return filters;
  }
}
