package com.ajanata.catbot;

import java.util.Properties;

import org.apache.log4j.Logger;


public interface Bot {
  static String PROP_NICKNAME = "nickname";
  static String PROP_OWNER_ID = "owner.id";
  static String PROP_TOKEN = "token";
  static String PROP_USERNAME = "username";

  boolean login();

  void shutdown();

  String getShortName();

  Properties getProperties();

  Logger logger();

  default String getProperty(final String key) {
    return getProperties().getProperty(key);
  }

  default String getToken() {
    return getProperty(getShortName() + "." + PROP_TOKEN);
  }

  default String getOwnerId() {
    return getProperty(getShortName() + "." + PROP_OWNER_ID);
  }

  default String getNickname() {
    return getProperty(getShortName() + "." + PROP_NICKNAME);
  }

  default String getUsername() {
    return getProperty(getShortName() + "." + PROP_USERNAME);
  }

  default void retry(final Runnable task) {
    int backoff = 500;
    while (true) {
      try {
        task.run();
        break;
      } catch (final Exception e) {
        logger().trace(String.format("Command failed in retry, backing off %d ms", backoff), e);
        try {
          Thread.sleep(backoff);
          backoff *= 2;
          if (backoff < 0) {
            backoff = Integer.MAX_VALUE;
          }
        } catch (final InterruptedException ee) {
          // pass
        }
      }
    }
  }
}
