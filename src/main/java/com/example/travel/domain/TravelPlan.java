package com.example.travel.domain;

import java.util.List;
import java.util.stream.Collectors;

public record TravelPlan(String summary, double totalEstimatedCost, List<DayPlan> days) {

  public record DayPlan(
      int dayNumber,
      String date,
      Accommodation accommodation,
      List<Transportation> transportation,
      List<Activity> activities,
      List<Meal> meals,
      double dailyEstimatedCost) {
  }

  public record Accommodation(String name, String description, double estimatedCost) {
  }

  public record Transportation(String type, String description, double estimatedCost) {
  }

  public record Activity(String name, String description, double estimatedCost, String timeOfDay) {
  }

  public record Meal(String type, String suggestion, double estimatedCost) {
  }

  public String asText(String destination, String startDate, String endDate, double budget) {
    var sb = new StringBuilder();
    sb.append("Travel Plan: ").append(destination).append("\n");
    sb.append(startDate).append(" - ").append(endDate).append("\n");
    sb.append(String.format("Budget: $%.2f%n%n", budget));
    sb.append("Summary: ").append(summary).append("\n\n");

    for (var day : days) {
      sb.append("Day ").append(day.dayNumber()).append(" - ").append(day.date()).append("\n");

      if (day.accommodation() != null) {
        sb.append("  Accommodation: ").append(day.accommodation().name())
            .append(" - ").append(day.accommodation().description())
            .append(String.format(" ($%.2f)", day.accommodation().estimatedCost()))
            .append("\n");
      }

      if (day.transportation() != null && !day.transportation().isEmpty()) {
        for (var t : day.transportation()) {
          sb.append("  Transportation: ").append(t.type())
              .append(" - ").append(t.description())
              .append(String.format(" ($%.2f)", t.estimatedCost()))
              .append("\n");
        }
      }

      if (day.activities() != null && !day.activities().isEmpty()) {
        sb.append("  Activities:\n");
        for (var a : day.activities()) {
          sb.append("    - ").append(capitalize(a.timeOfDay())).append(": ")
              .append(a.name()).append(" - ").append(a.description())
              .append(String.format(" ($%.2f)", a.estimatedCost()))
              .append("\n");
        }
      }

      if (day.meals() != null && !day.meals().isEmpty()) {
        sb.append("  Meals:\n");
        for (var m : day.meals()) {
          sb.append("    - ").append(capitalize(m.type())).append(": ")
              .append(m.suggestion())
              .append(String.format(" ($%.2f)", m.estimatedCost()))
              .append("\n");
        }
      }

      sb.append(String.format("  Daily Cost: $%.2f%n%n", day.dailyEstimatedCost()));
    }

    sb.append(String.format("Total Estimated Cost: $%.2f%n", totalEstimatedCost));
    return sb.toString();
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }
}
