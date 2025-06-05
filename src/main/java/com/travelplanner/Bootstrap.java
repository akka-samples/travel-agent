package com.travelplanner;


import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setup
public class Bootstrap implements ServiceSetup {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public Bootstrap() {
    var openAiApiKey = System.getenv("OPENAI_API_KEY");
    if (openAiApiKey == null || openAiApiKey.isEmpty()) {
      logger.error(
          "No API keys found. Make sure you have OPENAI_API_KEY defined as environment variable.");
      throw new RuntimeException("No API keys found.");
    }
  }

}

