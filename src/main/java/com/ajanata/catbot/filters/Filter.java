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
  FilterResult handleMessage(int botId, String fromName, String fromId, String chatId, String message);

  static class FilterResult {
    public final String message;
    public final boolean replyToPrevious;
    public FilterResult(final String message, final boolean replyToPrevious) {
      this.message = message;
      this.replyToPrevious = replyToPrevious;
    }
  }
}
