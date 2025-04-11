package com.travelplanner.domain;

/**
 * Represents a user's travel preference.
 * This class is designed to be serializable for event persistence.
 */
public record TravelPreference(
    PreferenceType type,
    String value,
    int priority
) {

  /**
   * Preference types that can be specified by users.
   */
  public enum PreferenceType {
    ACCOMMODATION_TYPE,  // e.g., "hotel", "hostel", "apartment"
    TRANSPORTATION_TYPE, // e.g., "flight", "train", "car"
    CUISINE,             // e.g., "italian", "vegetarian"
    ACTIVITY,            // e.g., "hiking", "museums", "beaches"
    CLIMATE,             // e.g., "warm", "cold", "moderate"
    BUDGET_RANGE         // e.g., "budget", "mid-range", "luxury"
  }
}
