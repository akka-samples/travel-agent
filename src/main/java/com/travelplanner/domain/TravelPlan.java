package com.travelplanner.domain;

import java.util.List;

public record TravelPlan(
    String summary,
    double totalEstimatedCost,
    List<DayPlan> days
) {

  public record DayPlan(
      int dayNumber,
      String date,
      Accommodation accommodation,
      List<Transportation> transportation,
      List<Activity> activities,
      List<Meal> meals,
      double dailyEstimatedCost
  ) {
  }

  public record Accommodation(
      String name,
      String description,
      double estimatedCost
  ) {
  }

  public record Transportation(
      String type,
      String description,
      double estimatedCost
  ) {
  }

  public record Activity(
      String name,
      String description,
      double estimatedCost,
      String timeOfDay
  ) {
  }

  public record Meal(
      String type,
      String suggestion,
      double estimatedCost
  ) {
  }
}
