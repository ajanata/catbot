package com.ajanata.catbot;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

public interface Retryable {
  Logger logger();

  default void retry(final Runnable task, @SuppressWarnings("rawtypes") Class... bailOn) {
    @SuppressWarnings("unchecked")
    final List<Class<? extends Throwable>> bailOnList = Arrays.asList(bailOn);
    int backoff = 500;
    while (true) {
      try {
        task.run();
        break;
      } catch (final Exception e) {
        // guaranteed to have a cause since it's wrapped... but whatever
        if (null != e.getCause() && bailOnList.contains(e.getCause().getClass())) {
          logger().info("Not retrying task, received a bailOn throwable", e);
          return;
        }
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
