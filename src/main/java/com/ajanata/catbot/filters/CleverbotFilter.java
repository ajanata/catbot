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
  public FilterResult filterMessage(final int botId, final String fromName, final String fromId,
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
