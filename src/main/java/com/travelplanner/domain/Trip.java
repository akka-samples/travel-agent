package com.travelplanner.domain;

import java.time.LocalDate;

/**
 * Domain model representing a trip with its itinerary.
 */
public record Trip(
    String tripId,
    String userId,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    double budget,
    TravelPlan generatedPlan,
    TripStatus status
) {
  /**
   * Factory method for creating a new trip.
   */
  public static Trip create(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      TravelPlan generatedPlan) {
    return new Trip(
        tripId,
        userId,
        destination,
        startDate,
        endDate,
        budget,
        generatedPlan,
        TripStatus.PLANNED
    );
  }

  /**
   * Returns a new Trip with updated plan.
   */
  public Trip withUpdatedPlan(TravelPlan newPlan) {
    return new Trip(
        tripId,
        userId,
        destination,
        startDate,
        endDate,
        budget,
        newPlan,
        status
    );
  }

  /**
   * Returns a new Trip with updated status.
   */
  public Trip withStatus(TripStatus newStatus) {
    return new Trip(
        tripId,
        userId,
        destination,
        startDate,
        endDate,
        budget,
        generatedPlan,
        newStatus
    );
  }

  /**
   * Trip status enum.
   */
  public enum TripStatus {
    PLANNED,  // Initial plan created
    BOOKED,   // Accommodations/transportation booked
    COMPLETED // Trip has been completed
  }
}
