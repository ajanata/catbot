package com.ajanata.catbot.handlers;

public class HelloHandler implements Handler {
  
	@Override
	public String handleMessage(final int botId, final String fromName, final String fromId, final String chatId, final String message) {
		return "Hello, " + fromName + "! You told me: " + message;
	}

  @Override
  public void init() {
    // do-nothing
  }
  
  @Override
  public String getDescription() {
    return "Says hello!";
  }
}
