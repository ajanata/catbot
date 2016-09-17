package com.ajanata.catbot.telegram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;
import org.telegram.telegrambots.bots.commands.ICommandRegistry;


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
    if (!chat.isUserChat())
      return;

    final StringBuilder helpMessageBuilder = new StringBuilder("<b>Help</b>\n");
    helpMessageBuilder.append("These are the registered commands for this Bot:\n\n");

    final List<BotCommand> commands = new ArrayList<>(commandRegistry.getRegisteredCommands());
    Collections.sort(commands,
        (left, right) -> left.getCommandIdentifier().compareTo(right.getCommandIdentifier()));

    for (BotCommand botCommand : commands) {
      helpMessageBuilder.append(botCommand.toString()).append("\n\n");
    }

    final SendMessage helpMessage = new SendMessage();
    helpMessage.setChatId(chat.getId().toString());
    helpMessage.enableHtml(true);
    helpMessage.setText(helpMessageBuilder.toString());

    try {
      absSender.sendMessage(helpMessage);
    } catch (final TelegramApiException e) {
      LOG.error("Unable to send help message to " + user.getUserName(), e);
    }
  }
}
