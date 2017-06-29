package com.ajanata.catbot.filters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.CatBot;
import com.ajanata.catbot.halbot.Halbot;
import com.ajanata.catbot.handlers.GetUserTweetHandler;
import com.ajanata.catbot.handlers.Handler;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

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
  private int brainSize = 0;
  private boolean ready = false;
  private int messagesSinceLastTalk = 0;
  private int retainPerChatCount = 0;
  private final Map<String, LinkedList<String>> retainPerChat = Collections.synchronizedMap(new HashMap<>());
  // TODO share instance?
  private Twitter twitter;

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
    if (ready) return;
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
    minThoughtLength = Integer.parseInt(catbot.getFilterProperty(filterId, "min.thought.length"));
    minThoughtWords = Integer.parseInt(catbot.getFilterProperty(filterId, "min.thought.words"));
    maxFreespeechInterval = Integer.parseInt(catbot.getFilterProperty(filterId, "max.freespeech.interval"));
    minFreespeechInterval = Integer.parseInt(catbot.getFilterProperty(filterId, "min.freespeech.interval"));
    retainPerChatCount = Integer.parseInt(catbot.getFilterProperty(filterId, "retention.chat.count"));

    twitter = new TwitterFactory().getInstance();

    final String clientKey = catbot.getProperty(GetUserTweetHandler.PROP_TWITTER_CLIENT_KEY);
    final String clientSecret = catbot.getProperty(GetUserTweetHandler.PROP_TWITTER_CLIENT_SECRET);
    twitter.setOAuthConsumer(clientKey, clientSecret);

    final String userToken = catbot.getProperty(GetUserTweetHandler.PROP_TWITTER_USER_TOKEN);
    final String userSecret = catbot.getProperty(GetUserTweetHandler.PROP_TWITTER_USER_SECRET);
    twitter.setOAuthAccessToken(new AccessToken(userToken, userSecret));

    ready = true;
  }

  private String think(final String thought) {
    LOG.trace(String.format("think(%s)", thought));
    messagesSinceLastTalk = 0;
    return halbot.getSentence(thought);
  }

  private String think() {
    LOG.trace("think()");
    messagesSinceLastTalk = 0;
    return halbot.getSentence();
  }

  private synchronized boolean learn(final String thought) {
    LOG.trace(String.format("learn(%s)", thought));
    if (null != brainWriter && thought.length() >= minThoughtLength && thought.split("\\s+").length >= minThoughtWords) {
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
      LinkedList<String> thoughts = retainPerChat.get(chatId);
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
    // TODO more
    if (message.toLowerCase(Locale.ENGLISH).contains(catbot.getBotProperty(botId, CatBot.PROP_NICKNAME).toLowerCase(Locale.ENGLISH))) {
      final String thought = think(message);
      retain(chatId, thought);
      return new FilterResult(thought, true);
    }

    messagesSinceLastTalk++;
    if (messagesSinceLastTalk > minFreespeechInterval
        && messagesSinceLastTalk - minFreespeechInterval >= random.nextInt(maxFreespeechInterval - minFreespeechInterval)) {
      LOG.debug(String.format("Randomly saying something after %d lines", messagesSinceLastTalk - 1));
      final String thought = think();
      retain(chatId, thought);
      return new FilterResult(thought, false);
    }
    return null;
  }

  @Override
  public String handleCommand(final int botId, final String fromName, final String fromId, final String chatId, final String trigger, final String message) {
    switch (trigger) {
      case "brains":
        return "Brain size: " + brainSize;
      case "tweet":
        final int index;
        if (message.trim().isEmpty()) {
          index = 0;
        } else {
          try {
            index = Integer.parseInt(message) - 1;
          } catch (final NumberFormatException e) {
            return "Invalid tweet reference number.";
          }
        }
        final String thought = retrieve(chatId, index);
        if (null == thought) {
          return "Invalid tweet reference number.";
        }
        // avoid tagging people
        thought.replaceAll("@", "");
        // TODO remove links?
        if (thought.length() > TWEET_MAX_LENGTH) {
          return "Tweet? Twit? Twot? Nope, too long.";
        }
        try {
          twitter.updateStatus(thought);
          return "Tweeted: " + thought;
        } catch (final TwitterException e) {
          LOG.error(String.format("Unable to tweet [%s]", thought), e);
          return "Unable to tweet: " + e.getMessage();
        }
    }
    return null;
  }

  @Override
  public String getDescription() {
    return "Retrieve how many lines are in the brain file.";
  }
}
