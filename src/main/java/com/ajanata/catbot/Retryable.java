package com.ajanata.catbot;

import org.slf4j.Logger;


public interface Retryable {
  Logger logger();

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
