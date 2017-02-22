package com.ajanata.catbot.filters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.CatBot;
import com.ajanata.catbot.halbot.Halbot;

public class HalbotFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(HalbotFilter.class);

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

  private final Halbot halbot = new Halbot();
  private final Random random = new Random();

  public HalbotFilter(final CatBot catbot, final int filterId) {
    this.catbot = catbot;
    this.filterId = filterId;
  }

  public static Filter createInstance(final CatBot catbot, final int filterId) {
    LOG.trace(String.format("createInstance(%d)", filterId));
    return new HalbotFilter(catbot, filterId);
  }

  @Override
  public void init() {
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
    halbot.train(thought);
    if (null != brainWriter && thought.length() >= minThoughtLength && thought.split("\\s+").length >= minThoughtWords) {
      try {
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

  @Override
  public FilterResult handleMessage(final int botId, final String fromName, final String fromId,
      final String chatId, final String message) {
    if (!ready) {
      return null;
    }
    if (!learn(message)) {
      // error writing to brain file, report that instead of thinking
      return new FilterResult("<error> Unable to learn previous message.", false);
    }
    if (message.startsWith(".brains")) {
      return new FilterResult("Brain size: " + brainSize, true);
    }
    // TODO more
    if (message.toLowerCase(Locale.ENGLISH).contains(catbot.getBotProperty(botId, CatBot.PROP_NICKNAME).toLowerCase(Locale.ENGLISH))) {
      return new FilterResult(think(message), true);
    }

    messagesSinceLastTalk++;
    if (messagesSinceLastTalk > minFreespeechInterval
        && messagesSinceLastTalk - minFreespeechInterval >= random.nextInt(maxFreespeechInterval - minFreespeechInterval)) {
      LOG.debug(String.format("Randomly saying something after %d lines", messagesSinceLastTalk - 1));
      return new FilterResult(think(), false);
    }
    return null;
  }
}
