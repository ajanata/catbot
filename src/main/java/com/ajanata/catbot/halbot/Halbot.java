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
    for (final String word : words) {
      if (word.length() > longest.length()) {
        longest = word;
      }
    }
    LOG.trace(String.format("getSentence(%s), using [%s]", prompt, longest));
    return hal.getSentence(longest);
  }
}
