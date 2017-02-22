package com.ajanata.catbot.halbot;

import org.jibble.jmegahal.JMegaHal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Halbot {
  private static final Logger LOG = LoggerFactory.getLogger(Halbot.class);

  private final JMegaHal hal = new JMegaHal();

  public void train(final String msg) {
    // TODO persist
    hal.add(msg);
  }

  // TODO load persisted

  public String getSentence() {
    return hal.getSentence();
  }

  /**
   *
   * @param prompt A complete sentence prompt. A word will be selected from this to prompt the brain. Currently, this is the longest word.
   * @return
   */
  public String getSentence(final String prompt) {
    final String[] words = prompt.split("\\s+");
    String longest = "";
    for (final String word: words) {
      if (word.length() > longest.length()) {
        longest = word;
      }
    }
    LOG.trace(String.format("getSentence(%s), using [%s]", prompt, longest));
    return hal.getSentence(longest);
  }
}
