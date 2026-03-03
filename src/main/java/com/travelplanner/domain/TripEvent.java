package com.travelplanner.domain;

import akka.javasdk.annotations.TypeName;

import java.time.LocalDate;

public sealed interface TripEvent {

  @TypeName("trip-created")
  record TripCreated(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      TravelPlan plan) implements TripEvent {
  }
}
