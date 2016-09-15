package com.ajanata.catbot.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ajanata.catbot.CatBot;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class GetUserTweetHandler implements Handler {
  private static final String HANDLER_PROP_TWITTER_USER = "twitter.user";
  private static final String PROP_TWITTER_CLIENT_KEY = "twitter.client.key";
  private static final String PROP_TWITTER_CLIENT_SECRET = "twitter.client.secret";
  private static final String PROP_TWITTER_USER_TOKEN = "twitter.user.token";
  private static final String PROP_TWITTER_USER_SECRET = "twitter.user.secret";

  private static final Logger LOG = LoggerFactory.getLogger(GetUserTweetHandler.class);
  
  private final CatBot catbot;
  private final int handlerId;
  // TODO expire entries
  private final Map<String, Long> newestTweet = new HashMap<>();
  
  private boolean initialized = false;
  private Twitter twitter;
  private String twitterUser;
  
  public GetUserTweetHandler(final CatBot catbot, final int handlerId) {
    this.catbot = catbot;
    this.handlerId = handlerId;
  }
  
  public static Handler createInstance(final CatBot catbot, final int handlerId) {
    LOG.trace(String.format("createInstance(%d)", handlerId));
    return new GetUserTweetHandler(catbot, handlerId);
  }

  @Override
  public void init() {
    if (initialized) return;
    
    twitter = new TwitterFactory().getInstance();
    
    final String clientKey = catbot.getProperty(PROP_TWITTER_CLIENT_KEY);
    final String clientSecret = catbot.getProperty(PROP_TWITTER_CLIENT_SECRET);
    twitter.setOAuthConsumer(clientKey, clientSecret);
    
    final String userToken = catbot.getProperty(PROP_TWITTER_USER_TOKEN);
    final String userSecret = catbot.getProperty(PROP_TWITTER_USER_SECRET);
    twitter.setOAuthAccessToken(new AccessToken(userToken, userSecret));
    
    twitterUser = catbot.getHandlerProperty(handlerId, HANDLER_PROP_TWITTER_USER);
    if (null == twitterUser || twitterUser.isEmpty()) {
      throw new RuntimeException("Handler " + handlerId + " does not have a twitter user configred.");
    }
    
    initialized = true;
  }

  @Override
  public String handleMessage(final int botId, final String fromName, final String fromId, final String chatId, final String message) {
    if (!initialized) throw new IllegalStateException("Handler not initialized.");
    
    if (fromId.equals(catbot.getBotProperty(botId, CatBot.PROP_OWNER_ID)) && "force".equals(message)) {
      LOG.info(String.format("Removing cached tweet ID for %s in chat %s requested by %s (owner), if any", twitterUser, chatId, fromName));
      newestTweet.remove(chatId);
    }
    
    final Paging paging;
    if (newestTweet.containsKey(chatId)) {
      final long sinceTweet = newestTweet.get(chatId);
      LOG.trace(String.format("Retrieving tweet since %d for %s in chat %s requested by %s", sinceTweet, twitterUser, chatId, fromName));
      paging = new Paging(1, 1, sinceTweet);
    } else {
      LOG.trace(String.format("Retreiving tweet for %s in new chat %s requested by %s", twitterUser, chatId, fromName));
      paging = new Paging(1, 1);
    }
    
    final ResponseList<Status> response;
    try {
      response = twitter.getUserTimeline(twitterUser, paging);
    } catch (final TwitterException e) {
      LOG.error("Unable to query Twitter for " + twitterUser, e);
      return "Unable to retrieve tweet for " + twitterUser + ": " + e.getMessage();
    }
    
    // we only deal with the first entry in this list
    for (final Status status: response) {
      final Duration sincePosted = Duration.between(status.getCreatedAt().toInstant(), Instant.now());
      final String since;
      final long days = sincePosted.toDays();
      final long hours = sincePosted.toHours() % 24;
      final long minutes = sincePosted.toMinutes() % 60;
      final long seconds = sincePosted.getSeconds() % 60;
      if (days > 0) {
        since = days + " day" + (days != 1 ? "s" : "");
      } else if (hours > 0) {
        since = hours + " hour" + (hours != 1 ? "s" : "");
      } else if (minutes > 0) {
        since = minutes + " minute" + (minutes != 1 ? "s" : "");
      } else {
        since = seconds + " second" + (seconds != 1 ? "s" : "");
      }
      
      final String text = status.getText();
      final StringBuilder builder = new StringBuilder();
      
      LOG.trace(String.format("Retrieved tweet for %s: %s", twitterUser, text));
      builder.append(twitterUser).append(" tweeted ").append(since).append(" ago:\n").append(text);
      
      newestTweet.put(chatId, status.getId());
      return builder.toString();
    }
    LOG.trace("No tweet received for " + twitterUser);
    return "No new tweets found.";
  }

  @Override
  public String getDescription() {
    return String.format("Shows the most recent tweet from %s.", twitterUser);
  }
  
  public static void main(String args[]) throws Exception{
    // The factory instance is re-useable and thread safe.
    Twitter twitter = TwitterFactory.getSingleton();
    twitter.setOAuthConsumer("key", "secret");
    RequestToken requestToken = twitter.getOAuthRequestToken();
    AccessToken accessToken = null;
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    while (null == accessToken) {
      System.out.println("Open the following URL and grant access to your account:");
      System.out.println(requestToken.getAuthorizationURL());
      System.out.print("Enter the PIN(if aviailable) or just hit enter.[PIN]:");
      String pin = br.readLine();
      try{
         if(pin.length() > 0){
           accessToken = twitter.getOAuthAccessToken(requestToken, pin);
         }else{
           accessToken = twitter.getOAuthAccessToken();
         }
      } catch (TwitterException te) {
        if(401 == te.getStatusCode()){
          System.out.println("Unable to get the access token.");
        }else{
          te.printStackTrace();
        }
      }
    }
    //persist to the accessToken for future reference.
    System.out.println("token:" + accessToken.getToken());
    System.out.println("token secret:" + accessToken.getTokenSecret());
    Status status = twitter.updateStatus("Mew!");
    System.out.println("Successfully updated the status to [" + status.getText() + "].");
    System.exit(0);
  }
}
