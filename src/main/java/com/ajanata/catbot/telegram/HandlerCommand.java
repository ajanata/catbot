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
  public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
    LOG.trace(String.format("%s: execute(%s, %s, %s, %s)", trigger, absSender, user, chat,
        Arrays.asList(arguments)));

    final String response = handler.handleMessage(botId, "@" + user.getUserName(), user.getId()
        .toString(), chat.getId().toString(), String.join(" ", arguments));
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
