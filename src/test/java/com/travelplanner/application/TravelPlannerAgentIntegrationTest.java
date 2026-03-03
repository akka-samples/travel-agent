package com.travelplanner.application;

import akka.javasdk.testkit.TestKit;
import akka.javasdk.testkit.TestKitSupport;
import akka.javasdk.testkit.TestModelProvider;
import com.travelplanner.domain.TravelPlan;
import akka.javasdk.JsonSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TravelPlannerAgentIntegrationTest extends TestKitSupport {

  private final TestModelProvider agentModel = new TestModelProvider();

  @Override
  protected TestKit.Settings testKitSettings() {
    return TestKit.Settings.DEFAULT
        .withModelProvider(TravelPlannerAgent.class, agentModel);
  }

  @Test
  void shouldGenerateTravelPlan() {
    // Create user first
    componentClient
        .forEventSourcedEntity("agent-test-user")
        .method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("Test User", "test@example.com"));

    // Mock the LLM response
    var mockPlan = new TravelPlan(
        "A wonderful Paris trip",
        1800.0,
        List.of(new TravelPlan.DayPlan(
            1, "2025-06-15",
            new TravelPlan.Accommodation("Hotel Le Marais", "Boutique hotel", 150.0),
            List.of(new TravelPlan.Transportation("flight", "CDG Airport arrival", 0.0)),
            List.of(new TravelPlan.Activity("Eiffel Tower", "Evening visit", 25.0, "evening")),
            List.of(new TravelPlan.Meal("dinner", "Le Petit Cler", 35.0)),
            210.0)));

    agentModel.fixedResponse(JsonSupport.encodeToString(mockPlan));

    var request = new TravelPlannerAgent.GenerateRequest(
        "agent-test-user", "Paris, France",
        LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
        2000.0);

    var result = componentClient
        .forAgent()
        .inSession("test-session")
        .method(TravelPlannerAgent::generateTravelPlan)
        .invoke(request);

    assertThat(result.summary()).isEqualTo("A wonderful Paris trip");
    assertThat(result.totalEstimatedCost()).isEqualTo(1800.0);
    assertThat(result.days()).hasSize(1);
    assertThat(result.days().getFirst().accommodation().name()).isEqualTo("Hotel Le Marais");
  }
}
