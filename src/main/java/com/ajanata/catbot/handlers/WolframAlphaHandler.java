package com.ajanata.catbot.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.CatBot;
import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;


public class WolframAlphaHandler implements Handler {

  private static final String PROP_APP_ID = "wolframalpha.api.appid";

  private static final Logger LOG = LoggerFactory.getLogger(WolframAlphaHandler.class);

  private final CatBot catbot;
  private final WAEngine engine = new WAEngine();
  private boolean initialized = false;

  public WolframAlphaHandler(final CatBot catbot) {
    this.catbot = catbot;
  }

  public static Handler createInstance(final CatBot catbot, final int handlerId) {
    LOG.trace(String.format("createInstance(%d)", handlerId));
    return new WolframAlphaHandler(catbot);
  }

  @Override
  public void init() {
    if (initialized)
      return;

    engine.setAppID(catbot.getProperty(PROP_APP_ID));
    engine.addFormat("plaintext");
    initialized = true;
  }

  @Override
  public String handleCommand(final int botId, final String fromName, final String fromId,
      final String chatId, final String trigger, final String message) {
    if (!initialized)
      throw new IllegalStateException("Handler not initialized.");
    LOG.trace(String.format("Querying Wolfram Alpha for %s in %s: %s", fromName, chatId, message));

    // TODO Async?
    final WAQuery query = engine.createQuery(message);
    final WAQueryResult result;
    try {
      result = engine.performQuery(query);
    } catch (final WAException e) {
      LOG.error(String.format("Unable to query Wolfram Alpha for %s in %s: %s", fromName, chatId,
          message), e);
      return String.format("I'm sorry, @%s, but I can't query Wolfram Alpha right now: %s",
          fromName, e.getMessage());
    }

    if (!result.isSuccess()) {
      LOG.trace("Error result.");
      return "Query was not understood; no results available.";
    }

    final StringBuilder builder = new StringBuilder();

    for (final WAPod pod : result.getPods()) {
      LOG.trace(String.format("Pod (%s): %s", pod.getClass().getSimpleName(), pod.getTitle()));
      if (pod.isError()) {
        // TODO better logging?
        LOG.trace(String.format("Error pod (%s): %s", pod.getClass().getSimpleName(), pod));
      } else {
        for (final WASubpod subpod : pod.getSubpods()) {
          LOG.trace(String.format("Subpod (%s): %s", subpod.getClass().getSimpleName(),
              subpod.getTitle()));
          for (final Object element : subpod.getContents()) {
            LOG.trace(String
                .format("Element (%s): %s", element.getClass().getSimpleName(), element));
            if (element instanceof WAPlainText) {
              final WAPlainText textElement = (WAPlainText) element;
              final String text = textElement.getText();
              LOG.trace(String.format("Plain text element: %s", text));
              if (null != text && !text.isEmpty()) {
                if (null != subpod.getTitle() && !subpod.getTitle().isEmpty()) {
                  builder.append(subpod.getTitle());
                } else {
                  builder.append(pod.getTitle());
                }
                builder.append(": ").append(text).append("\n");
              }
            }
          }
        }
      }
    }

    LOG.trace("Query results complete.");
    return builder.toString();
  }

  @Override
  public String getDescription() {
    return "Queries Wolfram Alpha. A query parameter is required.";
  }
}
