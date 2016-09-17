package com.ajanata.catbot.handlers;

public interface Handler {
  // Handlers can have a static method with signature Handler createInstance(CatBot,int). The CatBot instance CANNOT be accessed
  // during that method or the constructor, as it is not fully constructed. Wait until init(). The int is the handler id as
  // provided in the properties file, which can be used to retrieve configuration parameters.

  // perform initialization.
  void init();

  String handleMessage(int botId, String fromName, String fromId, String chatId, String message);

  String getDescription();
}
