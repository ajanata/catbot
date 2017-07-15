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

package com.ajanata.catbot.telegram;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;

import com.ajanata.catbot.Retryable;
import com.ajanata.catbot.handlers.Handler;
import com.diffplug.common.base.Errors;


public class HandlerCommand extends BotCommand implements Retryable {

  private static final Logger LOG = LoggerFactory.getLogger(HandlerCommand.class);

  private final int botId;
  private final String trigger;
  private final Handler handler;

  public HandlerCommand(final int botId, final String trigger, final Handler handler) {
    super(trigger, handler.getDescription());
    this.botId = botId;
    this.trigger = trigger;
    this.handler = handler;
  }

  @Override
  public void execute(final AbsSender absSender, final User user, final Chat chat,
      final String[] arguments) {
    LOG.trace(String.format("%s: execute(%s, %s, %s, %s)", trigger, absSender, user, chat,
        Arrays.asList(arguments)));

    final String response = handler.handleCommand(botId, "@" + user.getUserName(), user.getId()
        .toString(), chat.getTitle(), trigger, String.join(" ", arguments));
    if (null != response) {
      final SendMessage send = new SendMessage();
      send.setChatId(chat.getId().toString());
      send.setText(response);
      retry(Errors.rethrow().wrap(
          () -> {
            absSender.sendMessage(send);
          }));
    }
  }

  @Override
  public Logger logger() {
    return LOG;
  }

  @Override
  public String toString() {
    return "<b>" + COMMAND_INIT_CHARACTER + getCommandIdentifier() + "</b>\n"
        + handler.getDescription();
  }
}
