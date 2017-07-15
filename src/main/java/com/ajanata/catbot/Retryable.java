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

package com.ajanata.catbot;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;


public interface Retryable {
  Logger logger();

  default void retry(final Runnable task, @SuppressWarnings("rawtypes") final Class... bailOn) {
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
