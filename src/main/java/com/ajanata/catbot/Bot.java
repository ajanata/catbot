package com.ajanata.catbot;

public interface Bot extends Retryable {
  // Constructors require signature (CatBot,String) for bot management instance and bot name
  boolean login();
  // This MUST NOT THROW.
  void shutdown();
}
