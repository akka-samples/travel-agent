package com.travelplanner.application;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import com.travelplanner.domain.TravelPlan;
import com.travelplanner.domain.TravelPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent that generates personalized travel plans using LLM and user preferences.
 * Retrieves user profile and uses that as preferences in the request to the LLM.
 */
@ComponentId("travel-planner-agent")
public class TravelPlannerAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      """
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
      """.stripIndent();

  public record Request(
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget) {
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public TravelPlannerAgent(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  /**
   * Generate a travel plan for a user based on their profile and trip parameters.
   */
  public Effect<TravelPlan> generateTravelPlan(Request request) {

    logger.info("Generating travel plan for user {} to {}", request.userId, request.destination);

    // First, retrieve the user profile to get preferences
    var userProfile =
        componentClient.forEventSourcedEntity(request.userId)
            .method(UserProfileEntity::getUserProfile)
            .invoke();

    // Format user preferences for the prompt
    var preferencesText = formatUserPreferences(userProfile.preferences());

    // Calculate trip duration
    int tripDuration = (int) Duration.between(
        request.startDate.atStartOfDay(),
        request.endDate.atStartOfDay().plusDays(1)
    ).toDays();

    // Create a detailed trip description
    var tripDetails = String.format(
            """
            I need a travel plan with the following details:
            
            USER: %s
            DESTINATION: %s
            DATES: %s to %s (%d days)
            BUDGET: $%.2f

            USER PREFERENCES:
            %s

            Please create a detailed day-by-day itinerary that matches these preferences and budget.
            """.stripIndent(),
        userProfile.name(),
        request.destination,
        request.startDate,
        request.endDate,
        tripDuration,
        request.budget,
        preferencesText
    );

    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage(tripDetails)
        .responseAs(TravelPlan.class)
        .thenReply();
  }

  private String formatUserPreferences(List<TravelPreference> preferences) {
    if (preferences.isEmpty()) {
      return "No specific preferences provided.";
    }

    // Group preferences by type
    Map<TravelPreference.PreferenceType, List<TravelPreference>> groupedPreferences =
        preferences.stream()
            .collect(Collectors.groupingBy(TravelPreference::type));

    var sb = new StringBuilder();

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
