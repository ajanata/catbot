/**
 * Copyright (c) 2017, Andy Janata
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

package com.ajanata.catbot.handlers;

import java.util.ArrayList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.CatBot;


public class Magic8BallHandler implements Handler {

  private static final Logger LOG = LoggerFactory.getLogger(Magic8BallHandler.class);

  private final CatBot catbot;
  private final int handlerId;
  private final boolean initialized = false;
  private final Random random = new Random();
  private final ArrayList<String> responses = new ArrayList<>();

  public Magic8BallHandler(final CatBot catbot, final int handlerId) {
    this.catbot = catbot;
    this.handlerId = handlerId;
  }

  public static Handler createInstance(final CatBot catbot, final int handlerId) {
    LOG.trace(String.format("createInstance(%d)", handlerId));
    return new Magic8BallHandler(catbot, handlerId);
  }

  @Override
  public void init() {
    if (initialized) {
      return;
    }

    final int phraseCount = Integer.valueOf(catbot.getHandlerProperty(handlerId, "phrases"));
    for (int i = 0; i < phraseCount; i++) {
      responses.add(catbot.getHandlerProperty(handlerId, "phrases." + i));
    }
    responses.trimToSize();
  }

  @Override
  public String handleCommand(final int botId, final String fromName, final String fromId,
      final String chatId, final String trigger, final String message) {
    LOG.trace(String.format("handleCommand(%d, %s, %s, %s, %s, %s)", botId, fromName, fromId,
        chatId, trigger, message));
    return responses.get(random.nextInt(responses.size()));
  }

  @Override
  public String getDescription() {
    return "Peer into a Magic 8-Ball.";
  }
}
