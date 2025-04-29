package com.travelplanner.application;

import akka.javasdk.JsonSupport;
import akka.javasdk.client.ComponentClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.travelplanner.domain.TravelPlan;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.UserProfile;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent service that generates personalized travel plans using LLM and user preferences.
 * Retrieves user profile and uses that as preferences in the request to the LLM.
 */
public class TravelPlannerAgent {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;
  private final ChatLanguageModel chatModel;

  /**
   * Assistant interface for travel planning.
   */
  private interface TravelPlannerAssistant {
    @SystemMessage("""
        You are an expert travel planner assistant. Your job is to create detailed, personalized travel itineraries
        based on user preferences and trip parameters. Focus on providing practical, actionable plans that match
        the user's preferences for accommodations, transportation, activities, and cuisine.
                
        IMPORTANT: You must respond with a valid JSON object that follows this structure:
        {
          "summary": "Brief overview of the trip",
          "totalEstimatedCost": 1234.56,
          "days": [
            {
              "dayNumber": 1,
              "date": "YYYY-MM-DD",
              "accommodation": {
                "name": "Hotel/Hostel name",
                "description": "Brief description",
                "estimatedCost": 123.45
              },
              "transportation": [
                {
                  "type": "FLIGHT/TRAIN/BUS/etc",
                  "description": "From A to B",
                  "estimatedCost": 123.45
                }
              ],
              "activities": [
                {
                  "name": "Activity name",
                  "description": "Brief description",
                  "estimatedCost": 123.45,
                  "timeOfDay": "MORNING/AFTERNOON/EVENING"
                }
              ],
              "meals": [
                {
                  "type": "BREAKFAST/LUNCH/DINNER",
                  "suggestion": "Restaurant or food suggestion",
                  "estimatedCost": 123.45
                }
              ],
              "dailyEstimatedCost": 123.45
            }
          ]
        }
                
        Do not include any explanations or text outside of the JSON structure.
        """)
    String createTravelPlanJson(String tripDetails);
  }

  public TravelPlannerAgent(ComponentClient componentClient, ChatLanguageModel chatModel) {
    this.componentClient = componentClient;
    this.chatModel = chatModel;
  }

  /**
   * Generate a travel plan for a user based on their profile and trip parameters.
   */
  public TravelPlan generateTravelPlan(
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget) {

    logger.info("Generating travel plan for user {} to {}", userId, destination);

    // First, retrieve the user profile to get preferences
    var userProfile =
        componentClient.forEventSourcedEntity(userId)
            .method(UserProfileEntity::getUserProfile)
            .invoke();

    // Generate the travel plan using the LLM Assistant
    return createTravelPlanWithAssistant(userProfile, destination, startDate, endDate, budget);
  }

  private TravelPlan createTravelPlanWithAssistant(
      UserProfile userProfile,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget) {

    // Format user preferences for the prompt
    String preferencesText = formatUserPreferences(userProfile.preferences());

    // Calculate trip duration
    int tripDuration = (int) Duration.between(
        startDate.atStartOfDay(),
        endDate.atStartOfDay().plusDays(1)
    ).toDays();

    // Create a detailed trip description
    String tripDetails = String.format("""
            I need a travel plan with the following details:
                        
            USER: %s
            DESTINATION: %s
            DATES: %s to %s (%d days)
            BUDGET: $%.2f
                        
            USER PREFERENCES:
            %s
                        
            Please create a detailed day-by-day itinerary that matches these preferences and budget.
            """,
        userProfile.name(),
        destination,
        startDate,
        endDate,
        tripDuration,
        budget,
        preferencesText
    );

    // Create chat memory to maintain context
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
        .maxMessages(10)
        .build();

    // Create the assistant
    TravelPlannerAssistant assistant = AiServices.builder(TravelPlannerAssistant.class)
        .chatLanguageModel(chatModel)
        .chatMemory(chatMemory)
        .build();

    // Generate the travel plan using the assistant
    logger.info("Sending request to Assistant for user {}", userProfile.userId());
    String response = assistant.createTravelPlanJson(tripDetails);
    logger.info("Received travel plan from Assistant for user {}: {}", userProfile.userId(), response);

    try {
      return JsonSupport.getObjectMapper().readValue(response, TravelPlan.class);
    } catch (JsonProcessingException e) {
      logger.error("Failed to parse travel plan JSON: {}", response, e);
      throw new RuntimeException("Failed to parse travel plan JSON: " + e.getMessage());
    }
  }

  private String formatUserPreferences(List<TravelPreference> preferences) {
    if (preferences.isEmpty()) {
      return "No specific preferences provided.";
    }

    // Group preferences by type
    Map<TravelPreference.PreferenceType, List<TravelPreference>> groupedPreferences =
        preferences.stream()
            .collect(Collectors.groupingBy(TravelPreference::type));

    StringBuilder sb = new StringBuilder();

    // Format each preference type
    for (var entry : groupedPreferences.entrySet()) {
      sb.append("- ").append(formatPreferenceType(entry.getKey())).append(": ");

      String values = entry.getValue().stream()
          .map(pref -> pref.value() + " (priority: " + pref.priority() + ")")
          .collect(Collectors.joining(", "));

      sb.append(values).append("\n");
    }

    return sb.toString();
  }

  private String formatPreferenceType(TravelPreference.PreferenceType type) {
    return switch (type) {
      case ACCOMMODATION_TYPE -> "Accommodation";
      case TRANSPORTATION_TYPE -> "Transportation";
      case CUISINE -> "Food preferences";
      case ACTIVITY -> "Activities";
      case CLIMATE -> "Climate preferences";
      case BUDGET_RANGE -> "Budget level";
    };
  }
}
