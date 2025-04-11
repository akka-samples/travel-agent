package com.travelplanner;


import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import com.travelplanner.application.TravelPlannerAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setup
public class Bootstrap implements ServiceSetup {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final TravelPlannerAgent travelPlannerAgent;

  public Bootstrap(ComponentClient componentClient) {
    if (!KeyUtils.hasValidKeys()) {
      logger.error(
          "No API keys found. When running locally, make sure you have a " + ".env.local file located under " +
              "src/main/resources/ (see src/main/resources/.env.example). When running in production, " +
              "make sure you have OPENAI_API_KEY defined as environment variable.");
      throw new RuntimeException("No API keys found.");
    }

    this.travelPlannerAgent = new TravelPlannerAgent(componentClient);
  }

  @Override
  @SuppressWarnings("unchecked")
  public DependencyProvider createDependencyProvider() {
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

