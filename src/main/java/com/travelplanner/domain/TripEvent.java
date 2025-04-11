package com.travelplanner.domain;

import akka.javasdk.annotations.TypeName;

import java.time.LocalDate;

/**
 * Events for TripEntity state changes.
 */
public sealed interface TripEvent {

  /**
   * Event emitted when a trip is created.
   */
  @TypeName("trip-created")
  record TripCreated(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      TravelPlan generatedPlan
  ) implements TripEvent {
  }

  /**
   * Event emitted when a trip plan is updated.
   */
  @TypeName("trip-plan-updated")
  record TripPlanUpdated(
      String tripId,
      TravelPlan generatedPlan
  ) implements TripEvent {
  }

  /**
   * Event emitted when a trip is booked.
   */
  @TypeName("trip-booked")
  record TripBooked(
      String tripId
  ) implements TripEvent {
  }

  /**
   * Event emitted when a trip is completed.
   */
  @TypeName("trip-completed")
  record TripCompleted(
      String tripId
  ) implements TripEvent {
  }
}
