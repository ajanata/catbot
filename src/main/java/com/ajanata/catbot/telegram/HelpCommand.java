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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.bots.commandbot.commands.ICommandRegistry;
import org.telegram.telegrambots.exceptions.TelegramApiException;


public class HelpCommand extends BotCommand {
  private final Logger LOG = LoggerFactory.getLogger(HelpCommand.class);

  private final ICommandRegistry commandRegistry;

  public HelpCommand(final ICommandRegistry commandRegistry) {
    super("help", "Get all the commands this bot provides.");
    this.commandRegistry = commandRegistry;
  }

  @Override
  public void execute(final AbsSender absSender, final User user, final Chat chat,
      final String[] strings) {
    // only allow /help in private chats
    if (!chat.isUserChat()) {
      return;
    }

    final StringBuilder helpMessageBuilder = new StringBuilder("<b>Help</b>\n");
    helpMessageBuilder.append("These are the registered commands for this Bot:\n\n");

    final List<BotCommand> commands = new ArrayList<>(commandRegistry.getRegisteredCommands());
    Collections.sort(commands,
        (left, right) -> left.getCommandIdentifier().compareTo(right.getCommandIdentifier()));

    for (final BotCommand botCommand : commands) {
      helpMessageBuilder.append(botCommand.toString()).append("\n\n");
    }

    final SendMessage helpMessage = new SendMessage();
    helpMessage.setChatId(chat.getId().toString());
    helpMessage.enableHtml(true);
    helpMessage.setText(helpMessageBuilder.toString());

    try {
      absSender.execute(helpMessage);
    } catch (final TelegramApiException e) {
      LOG.error("Unable to send help message to " + user.getUserName(), e);
    }
  }
}
