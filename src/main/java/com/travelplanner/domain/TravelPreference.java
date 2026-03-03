package com.travelplanner.domain;

public record TravelPreference(PreferenceType type, String value, int priority) {

  public enum PreferenceType {
    ACCOMMODATION_TYPE,
    TRANSPORTATION_TYPE,
    CUISINE,
    ACTIVITY,
    CLIMATE,
    BUDGET_RANGE
  }
}
