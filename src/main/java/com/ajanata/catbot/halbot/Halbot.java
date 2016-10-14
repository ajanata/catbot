package com.ajanata.catbot.halbot;

import org.jibble.jmegahal.JMegaHal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Halbot {
  private static final Logger LOG = LoggerFactory.getLogger(Halbot.class);

  private final JMegaHal hal = new JMegaHal();

  public void train(final String msg) {
    hal.add(msg);
  }
}
