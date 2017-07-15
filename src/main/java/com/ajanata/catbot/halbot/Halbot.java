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

package com.ajanata.catbot.halbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.jibble.jmegahal.JMegaHal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Halbot {
  private static final Logger LOG = LoggerFactory.getLogger(Halbot.class);

  private final JMegaHal hal = new JMegaHal();
  private final Random random = new Random();

  public void train(final String msg) {
    // TODO persist
    hal.add(msg);
  }

  // TODO load persisted

  public String getSentence() {
    return hal.getSentence();
  }

  /**
   * @param prompt A complete sentence prompt. A word will be selected from this to prompt the
   * brain. Currently, this is weighted based on word length, ignoring 1- and 2-letter words.
   * @param lowWeightPrefixes Low-weight words in the prompt. Any word that starts with an entry in
   * this list will only be "worth" 1, no matter how many characters are in it. <strong>Must be
   * entirely lowercase.</strong>
   * @return A sentence based on the prompt.
   */
  public String getSentence(final String prompt, final String... lowWeightPrefixes) {
    final String[] words = prompt.split("\\s+");
    // allocate space for an average weight of 5 to start
    final List<String> weightedWords = new ArrayList<>(words.length * 5);

    for (final String word : words) {
      if (word.length() <= 2) {
        // skip short words
        continue;
      }
      boolean prefixed = false;
      for (final String prefix : lowWeightPrefixes) {
        if (word.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
          prefixed = true;
          break;
        }
      }
      if (prefixed) {
        weightedWords.add(word);
      } else {
        for (int i = 0; i < word.length(); i++) {
          weightedWords.add(word);
        }
      }
    }

    if (weightedWords.isEmpty()) {
      // should not happen in practice
      LOG.warn(String.format("getSentence(%s), somehow didn't chose a word!"));
      return hal.getSentence();
    } else {
      final String chosen = weightedWords.get(random.nextInt(weightedWords.size()));
      LOG.trace(String.format("getSentence(%s), using [%s]", prompt, chosen));
      return hal.getSentence(chosen);
    }
  }
}
