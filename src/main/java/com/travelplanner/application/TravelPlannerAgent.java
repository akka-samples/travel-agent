package com.travelplanner.application;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.Component;
import akka.javasdk.client.ComponentClient;
import com.travelplanner.domain.TravelPlan;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.UserProfile;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component(id = "travel-planner-agent")
public class TravelPlannerAgent extends Agent {

  private final ComponentClient componentClient;

  public TravelPlannerAgent(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public record GenerateRequest(
    String userId,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    double budget
  ) {}

  public Effect<TravelPlan> generateTravelPlan(GenerateRequest request) {
    UserProfile profile = null;
    try {
      profile = componentClient
        .forEventSourcedEntity(request.userId())
        .method(UserProfileEntity::getUserProfile)
        .invoke();
    } catch (Exception e) {
      // User may not have a profile; proceed without preferences
    }

    long tripDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate());
    String preferencesText = formatPreferences(profile);

    String systemMsg =
      """
      You are an expert travel planner. Create a detailed day-by-day travel itinerary.
      Return ONLY a valid JSON response conforming to the requested schema.
      Include realistic accommodation, transportation, activities, and meal recommendations.
      Ensure the total estimated cost stays within the given budget.
      Make recommendations that align with the user's preferences when provided.
      """;

    String userMsg = String.format(
      """
      Plan a %d-day trip to %s from %s to %s with a budget of $%.2f.
      %s
      Create a detailed day-by-day itinerary with accommodation, transportation, activities (with time of day), and meals for each day.
      Include estimated costs for each item and ensure the total stays within budget.
      """,
      tripDays,
      request.destination(),
      request.startDate(),
      request.endDate(),
      request.budget(),
      preferencesText
    );

    return effects()
      .systemMessage(systemMsg)
      .userMessage(userMsg)
      .responseConformsTo(TravelPlan.class)
      .onFailure(
        throwable ->
          new TravelPlan(
            "Unable to generate travel plan: " + throwable.getMessage(),
            0.0,
            List.of()
          )
      )
      .thenReply();
  }

  private String formatPreferences(UserProfile profile) {
    if (profile == null || profile.preferences().isEmpty()) {
      return "No specific preferences provided.";
    }

    Map<TravelPreference.PreferenceType, List<TravelPreference>> grouped = profile
      .preferences()
      .stream()
      .collect(Collectors.groupingBy(TravelPreference::type));

    var sb = new StringBuilder("User preferences:\n");
    if (profile.name() != null) {
      sb.append("- Name: ").append(profile.name()).append("\n");
    }

    grouped.forEach((type, prefs) -> {
      String label =
        switch (type) {
          case ACCOMMODATION_TYPE -> "Accommodation";
          case TRANSPORTATION_TYPE -> "Transportation";
          case CUISINE -> "Food & Dining";
          case ACTIVITY -> "Activities";
          case CLIMATE -> "Climate preference";
          case BUDGET_RANGE -> "Budget style";
        };
      sb.append("- ").append(label).append(": ");
      sb.append(
        prefs
          .stream()
          .map(p -> p.value() + " (priority: " + p.priority() + ")")
          .collect(Collectors.joining(", "))
      );
      sb.append("\n");
    });

    return sb.toString();
  }
}
