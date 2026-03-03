package com.example.travel.api;

import akka.javasdk.JsonSupport;
import akka.javasdk.testkit.TestKit;
import akka.javasdk.testkit.TestKitSupport;
import akka.javasdk.testkit.TestModelProvider;
import com.example.travel.api.TravelPlannerEndpoint.CreateTravelPlanRequest;
import com.example.travel.api.TravelPlannerEndpoint.CreateTravelPlanResponse;
import com.example.travel.api.TravelPlannerEndpoint.TripResponse;
import com.example.travel.api.UserProfileEndpoint.AddPreferenceRequest;
import com.example.travel.api.UserProfileEndpoint.CreateUserRequest;
import com.example.travel.application.TravelPlannerAgent;
import com.example.travel.domain.TravelPlan;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TravelPlannerEndpointIntegrationTest extends TestKitSupport {

  private final TestModelProvider agentModel = new TestModelProvider();

  @Override
  protected TestKit.Settings testKitSettings() {
    return TestKit.Settings.DEFAULT
        .withModelProvider(TravelPlannerAgent.class, agentModel);
  }

  private TravelPlan samplePlan() {
    return new TravelPlan(
        "A wonderful Paris trip",
        1800.0,
        List.of(new TravelPlan.DayPlan(
            1, "2025-06-15",
            new TravelPlan.Accommodation("Hotel Le Marais", "Boutique hotel", 150.0),
            List.of(new TravelPlan.Transportation("flight", "CDG Airport arrival", 0.0)),
            List.of(new TravelPlan.Activity("Eiffel Tower", "Evening visit", 25.0, "evening")),
            List.of(new TravelPlan.Meal("dinner", "Le Petit Cler", 35.0)),
            210.0)));
  }

  @Test
  void shouldCreateTravelPlanAndRetrieve() {
    var userId = "e2e-user-" + System.nanoTime();

    // Create user
    httpClient.POST("/users/" + userId)
        .withRequestBody(new CreateUserRequest("John", "john@example.com"))
        .invoke();

    // Add preference
    httpClient.POST("/users/" + userId + "/preferences")
        .withRequestBody(new AddPreferenceRequest("ACTIVITY", "museums", 5))
        .invoke();

    // Mock LLM
    agentModel.fixedResponse(JsonSupport.encodeToString(samplePlan()));

    // Create travel plan
    var createResponse = httpClient
        .POST("/travel-planner/create")
        .withRequestBody(new CreateTravelPlanRequest(
            userId, "Paris, France",
            LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
            2000.0))
        .responseBodyAs(CreateTravelPlanResponse.class)
        .invoke();

    assertThat(createResponse.status().intValue()).isEqualTo(201);
    var tripId = createResponse.body().tripId();
    assertThat(tripId).isNotNull();

    // Wait for workflow to complete and trip to be retrievable
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          var tripResponse = httpClient
              .GET("/travel-planner/trips/" + tripId)
              .responseBodyAs(TripResponse.class)
              .invoke();
          assertThat(tripResponse.status().isSuccess()).isTrue();
          assertThat(tripResponse.body().plan().summary()).isEqualTo("A wonderful Paris trip");
        });

    // Also retrieve as text
    var textResponse = httpClient
        .GET("/travel-planner/trips/" + tripId + "/as-text")
        .responseBodyAs(String.class)
        .invoke();
    assertThat(textResponse.status().isSuccess()).isTrue();
    assertThat(textResponse.body()).contains("Paris, France");
    assertThat(textResponse.body()).contains("Hotel Le Marais");
  }

  @Test
  void shouldRejectInvalidDates() {
    var response = httpClient
        .POST("/travel-planner/create")
        .withRequestBody(new CreateTravelPlanRequest(
            "user-1", "Paris",
            LocalDate.of(2025, 6, 22), LocalDate.of(2025, 6, 15),
            2000.0))
        .invoke();

    assertThat(response.status().intValue()).isEqualTo(400);
  }

  @Test
  void shouldReturn404ForNonExistentTrip() {
    var response = httpClient
        .GET("/travel-planner/trips/non-existent-trip-id")
        .invoke();

    assertThat(response.status().intValue()).isEqualTo(404);
  }

  @Test
  void shouldReturn404ForNonExistentTripAsText() {
    var response = httpClient
        .GET("/travel-planner/trips/non-existent-trip-id/as-text")
        .invoke();

    assertThat(response.status().intValue()).isEqualTo(404);
  }

  @Test
  void shouldRejectNegativeBudget() {
    var response = httpClient
        .POST("/travel-planner/create")
        .withRequestBody(new CreateTravelPlanRequest(
            "user-1", "Paris",
            LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
            -100.0))
        .invoke();

    assertThat(response.status().intValue()).isEqualTo(400);
  }
}
