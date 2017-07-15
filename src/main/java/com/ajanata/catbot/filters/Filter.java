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

public interface Filter {
  // Filters can have a static method with signature Filter createInstance(CatBot,int). The CatBot instance CANNOT be accessed
  // during that method or the constructor, as it is not fully constructed. Wait until init(). The int is the filter id as
  // provided in the properties file, which can be used to retrieve configuration parameters.

  // perform initialization.
  void init();

  /**
   *
   * @param botId
   * @param fromName
   * @param fromId
   * @param chatId
   * @param message
   * @return {@code null} if no response, otherwise message to send as response. The first filter that returns a response causes no further filters to be called.
   */
  FilterResult filterMessage(int botId, String fromName, String fromId, String chatId,
      String message);

  static class FilterResult {
    public final String message;
    public final boolean replyToPrevious;

    public FilterResult(final String message, final boolean replyToPrevious) {
      this.message = message;
      this.replyToPrevious = replyToPrevious;
    }
  }
}
