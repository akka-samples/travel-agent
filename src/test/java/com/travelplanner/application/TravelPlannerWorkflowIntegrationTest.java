package com.travelplanner.application;

import akka.javasdk.JsonSupport;
import akka.javasdk.testkit.TestKit;
import akka.javasdk.testkit.TestKitSupport;
import akka.javasdk.testkit.TestModelProvider;
import com.travelplanner.domain.TravelPlan;
import com.travelplanner.domain.Trip;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TravelPlannerWorkflowIntegrationTest extends TestKitSupport {

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
  void shouldCompleteWorkflowSuccessfully() {
    var userId = "wf-user-" + System.nanoTime();
    var tripId = "wf-trip-" + System.nanoTime();

    // Create user first
    componentClient
        .forEventSourcedEntity(userId)
        .method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("Test User", "test@example.com"));

    // Mock LLM response
    agentModel.fixedResponse(JsonSupport.encodeToString(samplePlan()));

    // Start workflow
    componentClient
        .forWorkflow(tripId)
        .method(TravelPlannerWorkflow::createTravelPlan)
        .invoke(new TravelPlannerWorkflow.CreateCommand(
            tripId, userId, "Paris, France",
            LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
            2000.0));

    // Wait for workflow to complete
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          var status = componentClient
              .forWorkflow(tripId)
              .method(TravelPlannerWorkflow::getStatus)
              .invoke();
          assertThat(status.status()).isEqualTo(
              TravelPlannerWorkflow.WorkflowStatus.COMPLETED);
        });

    // Verify trip was stored
    var trip = componentClient
        .forEventSourcedEntity(tripId)
        .method(TripEntity::getTrip)
        .invoke();
    assertThat(trip.destination()).isEqualTo("Paris, France");
    assertThat(trip.plan().summary()).isEqualTo("A wonderful Paris trip");
    assertThat(trip.status()).isEqualTo(Trip.TripStatus.PLANNED);

    // Verify user profile was updated with trip ID
    var userProfile = componentClient
        .forEventSourcedEntity(userId)
        .method(UserProfileEntity::getUserProfile)
        .invoke();
    assertThat(userProfile.pastTripIds()).contains(tripId);
  }

  @Test
  void shouldRejectDuplicateWorkflowStart() {
    var userId = "wf-dup-user-" + System.nanoTime();
    var tripId = "wf-dup-trip-" + System.nanoTime();

    // Create user first
    componentClient
        .forEventSourcedEntity(userId)
        .method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("Dup User", "dup@example.com"));

    // Mock LLM response
    agentModel.fixedResponse(JsonSupport.encodeToString(samplePlan()));

    var command = new TravelPlannerWorkflow.CreateCommand(
        tripId, userId, "Paris, France",
        LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
        2000.0);

    // Start workflow first time
    componentClient
        .forWorkflow(tripId)
        .method(TravelPlannerWorkflow::createTravelPlan)
        .invoke(command);

    // Wait for completion
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          var status = componentClient
              .forWorkflow(tripId)
              .method(TravelPlannerWorkflow::getStatus)
              .invoke();
          assertThat(status.status()).isEqualTo(
              TravelPlannerWorkflow.WorkflowStatus.COMPLETED);
        });
  }
}
