package com.travelplanner;


import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import com.travelplanner.application.TravelPlannerAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Setup
public class Bootstrap implements ServiceSetup {
  final private static OpenAiChatModelName chatModelName = OpenAiChatModelName.GPT_4_O_MINI;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public Bootstrap(ComponentClient componentClient) {
    this.componentClient = componentClient;
    if (!KeyUtils.hasValidKeys()) {
      logger.error(
          "No API keys found. When running locally, make sure you have a " + ".env.local file located under " +
              "src/main/resources/ (see src/main/resources/.env.example). When running in production, " +
              "make sure you have OPENAI_API_KEY defined as environment variable.");
      throw new RuntimeException("No API keys found.");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public DependencyProvider createDependencyProvider() {
    var chatModel = OpenAiChatModel.builder()
        .modelName(chatModelName)
        .apiKey(KeyUtils.readOpenAiKey())
        .timeout(Duration.ofSeconds(60))
        .logRequests(true)
        .logResponses(true)
        .build();
    var travelPlannerAgent = new TravelPlannerAgent(componentClient, chatModel);

    return new DependencyProvider() {
      @Override
      public <T> T getDependency(Class<T> cls) {
        if (cls.equals(TravelPlannerAgent.class)) {
          return (T) travelPlannerAgent;
        }

        return null;
      }
    };
  }

}

