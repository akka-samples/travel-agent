package com.travelplanner.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;
import com.travelplanner.domain.TravelPlan;
import com.travelplanner.domain.Trip;
import com.travelplanner.domain.TripEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TripEntityTest {

  private final String tripId = "trip-123";
  private final String userId = "user-456";
  private final String destination = "Paris, France";
  private final LocalDate startDate = LocalDate.of(2023, 6, 15);
  private final LocalDate endDate = LocalDate.of(2023, 6, 22);
  private final double budget = 2000.0;

  private TravelPlan createSampleTravelPlan() {
    // Create a sample day plan
    TravelPlan.Accommodation accommodation = new TravelPlan.Accommodation(
        "Hotel Paris", "Lovely hotel in the city center", 150.0);

    List<TravelPlan.Transportation> transportation = List.of(
        new TravelPlan.Transportation("TAXI", "Airport to Hotel", 50.0));

    List<TravelPlan.Activity> activities = List.of(
        new TravelPlan.Activity("Eiffel Tower", "Visit the iconic landmark", 25.0, "MORNING"),
        new TravelPlan.Activity("Louvre Museum", "Explore the famous museum", 15.0, "AFTERNOON"));

    List<TravelPlan.Meal> meals = List.of(
        new TravelPlan.Meal("BREAKFAST", "Café de Paris", 15.0),
        new TravelPlan.Meal("LUNCH", "Bistro Parisien", 30.0),
        new TravelPlan.Meal("DINNER", "Restaurant Eiffel", 45.0));

    TravelPlan.DayPlan dayPlan = new TravelPlan.DayPlan(
        1, "2023-06-15", accommodation, transportation, activities, meals, 330.0);

    // Create the travel plan with the day plan
    return new TravelPlan(
        "A week in Paris", 2000.0, List.of(dayPlan));
  }

  @Test
  public void testCreateTrip() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);

    TravelPlan travelPlan = createSampleTravelPlan();

    var result = testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, travelPlan));

    // Assert the reply is Done
    assertEquals(Done.done(), result.getReply());

    // Assert the event was persisted
    var tripCreatedEvent = result.getNextEventOfType(TripEvent.TripCreated.class);
    assertEquals(tripId, tripCreatedEvent.tripId());
    assertEquals(userId, tripCreatedEvent.userId());
    assertEquals(destination, tripCreatedEvent.destination());
    assertEquals(startDate, tripCreatedEvent.startDate());
    assertEquals(endDate, tripCreatedEvent.endDate());
    assertEquals(budget, tripCreatedEvent.budget());
    assertEquals(travelPlan, tripCreatedEvent.generatedPlan());

    // Assert the state was updated
    var state = testKit.method(TripEntity::getTrip).invoke().getReply();
    assertEquals(tripId, state.tripId());
    assertEquals(userId, state.userId());
    assertEquals(destination, state.destination());
    assertEquals(startDate, state.startDate());
    assertEquals(endDate, state.endDate());
    assertEquals(budget, state.budget());
    assertEquals(travelPlan, state.generatedPlan());
    assertEquals(Trip.TripStatus.PLANNED, state.status());
  }

  @Test
  public void testCreateTripIdempotent() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);

    TravelPlan travelPlan = createSampleTravelPlan();

    // First creation
    testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, travelPlan));

    // Second creation with same ID should be idempotent
    var result = testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, travelPlan));

    assertEquals(Done.done(), result.getReply());
    assertFalse(result.didPersistEvents());
  }

  @Test
  public void testUpdateTripPlan() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);

    // Create the trip first
    TravelPlan initialPlan = createSampleTravelPlan();
    testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, initialPlan));

    // Create an updated plan
    TravelPlan updatedPlan = new TravelPlan(
        "Updated Paris trip", 2200.0, new ArrayList<>(initialPlan.days()));

    // Update the trip plan
    var result = testKit.method(TripEntity::updateTripPlan)
        .invoke(new TripEntity.UpdateTripPlan(updatedPlan));

    // Assert the reply is Done
    assertEquals(Done.done(), result.getReply());

    // Assert the event was persisted
    var tripPlanUpdatedEvent = result.getNextEventOfType(TripEvent.TripPlanUpdated.class);
    assertEquals(tripId, tripPlanUpdatedEvent.tripId());
    assertEquals(updatedPlan, tripPlanUpdatedEvent.generatedPlan());

    // Assert the state was updated
    var state = testKit.method(TripEntity::getTrip).invoke().getReply();
    assertEquals(updatedPlan, state.generatedPlan());
    assertEquals(Trip.TripStatus.PLANNED, state.status()); // Status should not change
  }

  @Test
  public void testMarkTripAsBooked() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);

    // Create the trip first
    TravelPlan travelPlan = createSampleTravelPlan();
    testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, travelPlan));

    // Mark the trip as booked
    var result = testKit.method(TripEntity::markTripAsBooked)
        .invoke(new TripEntity.MarkTripAsBooked());

    // Assert the reply is Done
    assertEquals(Done.done(), result.getReply());

    // Assert the event was persisted
    var tripBookedEvent = result.getNextEventOfType(TripEvent.TripBooked.class);
    assertEquals(tripId, tripBookedEvent.tripId());

    // Assert the state was updated
    var state = testKit.method(TripEntity::getTrip).invoke().getReply();
    assertEquals(Trip.TripStatus.BOOKED, state.status());
  }

  @Test
  public void testMarkTripAsBookedIdempotent() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);

    // Create the trip and mark as booked
    TravelPlan travelPlan = createSampleTravelPlan();
    testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, travelPlan));
    testKit.method(TripEntity::markTripAsBooked)
        .invoke(new TripEntity.MarkTripAsBooked());

    // Try to mark as booked again
    var result = testKit.method(TripEntity::markTripAsBooked)
        .invoke(new TripEntity.MarkTripAsBooked());

    assertEquals(Done.done(), result.getReply());
    assertFalse(result.didPersistEvents());
  }

  @Test
  public void testMarkTripAsCompleted() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);

    // Create the trip first
    TravelPlan travelPlan = createSampleTravelPlan();
    testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, travelPlan));

    // Mark the trip as completed
    var result = testKit.method(TripEntity::markTripAsCompleted)
        .invoke(new TripEntity.MarkTripAsCompleted());

    // Assert the reply is Done
    assertEquals(Done.done(), result.getReply());

    // Assert the event was persisted
    var tripCompletedEvent = result.getNextEventOfType(TripEvent.TripCompleted.class);
    assertEquals(tripId, tripCompletedEvent.tripId());

    // Assert the state was updated
    var state = testKit.method(TripEntity::getTrip).invoke().getReply();
    assertEquals(Trip.TripStatus.COMPLETED, state.status());
  }

  @Test
  public void testMarkTripAsCompletedIdempotent() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);

    // Create the trip and mark as completed
    TravelPlan travelPlan = createSampleTravelPlan();
    testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, travelPlan));
    testKit.method(TripEntity::markTripAsCompleted)
        .invoke(new TripEntity.MarkTripAsCompleted());

    // Try to mark as completed again
    var result = testKit.method(TripEntity::markTripAsCompleted)
        .invoke(new TripEntity.MarkTripAsCompleted());

    assertEquals(Done.done(), result.getReply());
    assertFalse(result.didPersistEvents());
  }

  @Test
  public void testGetTripNotFound() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);
    var result = testKit.method(TripEntity::getTrip).invoke();
    assertTrue(result.isError());
  }

  @Test
  public void testUpdateTripPlanNotFound() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);
    TravelPlan travelPlan = createSampleTravelPlan();
    var result = testKit.method(TripEntity::updateTripPlan)
        .invoke(new TripEntity.UpdateTripPlan(travelPlan));
    assertTrue(result.isError());
  }

  @Test
  public void testMarkTripAsBookedNotFound() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);
    var result = testKit.method(TripEntity::markTripAsBooked)
        .invoke(new TripEntity.MarkTripAsBooked());
    assertTrue(result.isError());
  }

  @Test
  public void testMarkTripAsCompletedNotFound() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);
    var result = testKit.method(TripEntity::markTripAsCompleted)
        .invoke(new TripEntity.MarkTripAsCompleted());
    assertTrue(result.isError());
  }

  @Test
  public void testFullTripLifecycle() {
    EventSourcedTestKit<Trip, TripEvent, TripEntity> testKit =
        EventSourcedTestKit.of(TripEntity::new);
    // 1. Create the trip
    TravelPlan initialPlan = createSampleTravelPlan();
    testKit.method(TripEntity::createTrip)
        .invoke(new TripEntity.CreateTrip(
            tripId, userId, destination, startDate, endDate, budget, initialPlan));

    // 2. Update the plan
    TravelPlan updatedPlan = new TravelPlan(
        "Updated Paris trip", 2200.0, new ArrayList<>(initialPlan.days()));
    testKit.method(TripEntity::updateTripPlan)
        .invoke(new TripEntity.UpdateTripPlan(updatedPlan));

    // 3. Mark as booked
    testKit.method(TripEntity::markTripAsBooked)
        .invoke(new TripEntity.MarkTripAsBooked());

    // 4. Mark as completed
    testKit.method(TripEntity::markTripAsCompleted)
        .invoke(new TripEntity.MarkTripAsCompleted());

    // Verify final state
    var finalState = testKit.method(TripEntity::getTrip).invoke().getReply();
    assertEquals(tripId, finalState.tripId());
    assertEquals(userId, finalState.userId());
    assertEquals(destination, finalState.destination());
    assertEquals(startDate, finalState.startDate());
    assertEquals(endDate, finalState.endDate());
    assertEquals(budget, finalState.budget());
    assertEquals(updatedPlan, finalState.generatedPlan());
    assertEquals(Trip.TripStatus.COMPLETED, finalState.status());

    // Verify all events were persisted
    assertEquals(4, testKit.getAllEvents().size());
  }
}
