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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.CatBot;
import com.ajanata.catbot.halbot.Halbot;
import com.ajanata.catbot.handlers.Handler;


// Singleton
public class HalbotFilterHandler implements Filter, Handler {
  private static final int TWEET_MAX_LENGTH = 140;

  private static final Logger LOG = LoggerFactory.getLogger(HalbotFilterHandler.class);

  private final CatBot catbot;
  private final int filterId;

  private BufferedWriter brainWriter;
  private int minThoughtLength;
  private int minThoughtWords;
  private int maxFreespeechInterval;
  private int minFreespeechInterval;
  private final Set<String> freespeechBlacklist = Collections.synchronizedSet(new HashSet<>());
  private int brainSize = 0;
  private boolean ready = false;
  private final Map<String, Integer> messagesSinceLastTalkPerChat = Collections
      .synchronizedMap(new HashMap<>());
  private int retainPerChatCount = 0;
  private final Map<String, LinkedList<String>> retainPerChat = Collections
      .synchronizedMap(new HashMap<>());

  private final Halbot halbot = new Halbot();
  private final Random random = new Random();

  private static HalbotFilterHandler instance;

  public HalbotFilterHandler(final CatBot catbot, final int filterId) {
    this.catbot = catbot;
    this.filterId = filterId;
  }

  public static synchronized Filter createInstance(final CatBot catbot, final int filterId) {
    if (null != instance) {
      return instance;
    }
    LOG.trace(String.format("createInstance(%d)", filterId));
    instance = new HalbotFilterHandler(catbot, filterId);
    return instance;
  }

  @Override
  public synchronized void init() {
    // only do this once, we get called for the filter and for each handler
    if (ready) {
      return;
    }
    // TODO move this to Halbot
    final String brainPath = catbot.getFilterProperty(filterId, "brain.path");
    try (final BufferedReader reader = new BufferedReader(new FileReader(brainPath))) {
      LOG.info("Loading brain from " + brainPath);
      String line;
      while ((line = reader.readLine()) != null) {
        brainSize++;
        halbot.train(line);
      }
      LOG.info("Done loading brain, size " + brainSize);
    } catch (final IOException e) {
      LOG.error(String.format("Unable to load brain from [%s]", brainPath), e);
      return;
    }
    try {
      brainWriter = new BufferedWriter(new FileWriter(brainPath, true));
      LOG.info(String.format("Appending to brain file [%s]", brainPath));
    } catch (final IOException e) {
      LOG.error(String.format("Unable to open file [%s] for brain writing", brainPath), e);
    }
    minThoughtLength = Integer.parseInt(catbot.getFilterProperty(filterId, "thought.min.length"));
    minThoughtWords = Integer.parseInt(catbot.getFilterProperty(filterId, "thought.min.words"));
    maxFreespeechInterval = Integer.parseInt(catbot.getFilterProperty(filterId,
        "freespeech.max.interval"));
    minFreespeechInterval = Integer.parseInt(catbot.getFilterProperty(filterId,
        "freespeech.min.interval"));
    retainPerChatCount = Integer.parseInt(catbot
        .getFilterProperty(filterId, "retention.chat.count"));

    for (int i = 0; i < Integer
        .parseInt(catbot.getFilterProperty(filterId, "freespeech.blacklist")); i++) {
      freespeechBlacklist.add(catbot.getFilterProperty(filterId, "freespeech.blacklist." + i));
    }

    ready = true;
  }

  private String think(final String thought) {
    LOG.trace(String.format("think(%s)", thought));
    return halbot.getSentence(thought, "catbot", "http");
  }

  private String think() {
    LOG.trace("think()");
    return halbot.getSentence();
  }

  private synchronized boolean learn(final String thought) {
    LOG.trace(String.format("learn(%s)", thought));
    if (null != brainWriter && thought.length() >= minThoughtLength
        && thought.split("\\s+").length >= minThoughtWords) {
      try {
        halbot.train(thought);
        brainWriter.append(thought);
        brainWriter.newLine();
        brainWriter.flush();
        brainSize++;
        LOG.trace("Saved to brain: " + thought);
      } catch (final IOException e) {
        LOG.error("Unable to write thought to brain file", e);
        brainWriter = null;
        ready = false;
        return false;
      }
    }
    return true;
  }

  private void retain(final String chatId, final String thought) {
    synchronized (retainPerChat) {
      LinkedList<String> thoughts = retainPerChat.get(chatId);
      if (null == thoughts) {
        // TODO CircularFifoQueue?
        thoughts = new LinkedList<>();
        retainPerChat.put(chatId, thoughts);
      }
      thoughts.addLast(thought);
      if (thoughts.size() > retainPerChatCount) {
        thoughts.removeFirst();
      }
    }
  }

  private String retrieve(final String chatId, final int index) {
    synchronized (retainPerChat) {
      final LinkedList<String> thoughts = retainPerChat.get(chatId);
      if (null == thoughts) {
        LOG.trace(String.format("No remembered thoughts for chat %s", chatId));
        return null;
      }
      try {
        // new ones are at the end
        return thoughts.get(thoughts.size() - index - 1);
      } catch (final IndexOutOfBoundsException e) {
        LOG.trace(String.format("Invalid thought index %d for chat %s", index, chatId));
        return null;
      }
    }
  }

  @Override
  public FilterResult filterMessage(final int botId, final String fromName, final String fromId,
      final String chatId, final String message) {
    if (!ready) {
      return null;
    }
    if (!learn(message)) {
      // error writing to brain file, report that instead of thinking
      return new FilterResult("<error> Unable to learn previous message.", false);
    }
    // TODO more?
    if (message.toLowerCase(Locale.ENGLISH).contains(
        catbot.getBotProperty(botId, CatBot.PROP_NICKNAME).toLowerCase(Locale.ENGLISH))) {
      return saySomethingPrompted(chatId, message);
    }

    final Integer sinceLastObj = messagesSinceLastTalkPerChat.get(chatId);
    int messagesSinceLastTalk = 0;
    if (null != sinceLastObj) {
      messagesSinceLastTalk = sinceLastObj;
    }
    messagesSinceLastTalkPerChat.put(chatId, ++messagesSinceLastTalk);
    if (!freespeechBlacklist.contains(chatId) && messagesSinceLastTalk > minFreespeechInterval
        && messagesSinceLastTalk - minFreespeechInterval >= random.nextInt(maxFreespeechInterval
            - minFreespeechInterval)) {
      LOG.debug(String.format("Randomly saying something in [%s] after %d lines", chatId,
          messagesSinceLastTalk - 1));
      return saySomethingRandom(chatId);
    }
    return null;
  }

  private FilterResult saySomethingPrompted(final String chatId, final String message) {
    messagesSinceLastTalkPerChat.put(chatId, 0);
    final String thought = think(message);
    retain(chatId, thought);
    return new FilterResult(thought, true);
  }

  private FilterResult saySomethingRandom(final String chatId) {
    messagesSinceLastTalkPerChat.put(chatId, 0);
    final String thought = think();
    retain(chatId, thought);
    return new FilterResult(thought, false);
  }

  @Override
  public String handleCommand(final int botId, final String fromName, final String fromId,
      final String chatId, final String trigger, final String message) {
    switch (trigger) {
      case "brains":
        return "Brain size: " + brainSize;
      case "tweet":
        return tweetPreviousThought(chatId, message);
    }
    return null;
  }

  private String tweetPreviousThought(final String chatId, final String message) {
    return "Twitter is fascist.";
  }

  // This doesn't work right with multiple commands in the same handler...
  @Override
  public String getDescription() {
    return "Retrieve how many lines are in the brain file.";
  }
}
