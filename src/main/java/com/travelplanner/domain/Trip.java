package com.travelplanner.domain;

import java.time.LocalDate;

public record Trip(
  String tripId,
  String userId,
  String destination,
  LocalDate startDate,
  LocalDate endDate,
  double budget,
  TravelPlan plan,
  TripStatus status
) {
  public enum TripStatus {
    PLANNED,
    BOOKED,
    COMPLETED,
  }

  public static Trip create(
    String tripId,
    String userId,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    double budget,
    TravelPlan plan
  ) {
    return new Trip(
      tripId,
      userId,
      destination,
      startDate,
      endDate,
      budget,
      plan,
      TripStatus.PLANNED
    );
  }

  public Trip withStatus(TripStatus status) {
    return new Trip(tripId, userId, destination, startDate, endDate, budget, plan, status);
  }

  public Trip withPlan(TravelPlan plan) {
    return new Trip(tripId, userId, destination, startDate, endDate, budget, plan, status);
  }
}
