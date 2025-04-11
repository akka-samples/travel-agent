package com.travelplanner.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain model representing a user profile with travel preferences.
 */
public record UserProfile(
    String userId,
    String name,
    String email,
    List<TravelPreference> preferences,
    List<String> pastTripIds
) {
  /**
   * Constructor with defensive copying for mutable collections.
   */
  public UserProfile {
    preferences = preferences != null ? new ArrayList<>(preferences) : new ArrayList<>();
    pastTripIds = pastTripIds != null ? new ArrayList<>(pastTripIds) : new ArrayList<>();
  }

  /**
   * Factory method for creating a new user profile.
   */
  public static UserProfile create(String userId, String name, String email) {
    return new UserProfile(userId, name, email, new ArrayList<>(), new ArrayList<>());
  }

  /**
   * Returns a new UserProfile with updated name and email.
   */
  public UserProfile withNameAndEmail(String newName, String newEmail) {
    return new UserProfile(userId, newName, newEmail, preferences, pastTripIds);
  }

  /**
   * Returns a new UserProfile with an added preference.
   */
  public UserProfile withAddedPreference(TravelPreference preference) {
    List<TravelPreference> updatedPreferences = new ArrayList<>(preferences);
    updatedPreferences.add(preference);
    return new UserProfile(userId, name, email, updatedPreferences, pastTripIds);
  }

  /**
   * Returns a new UserProfile with an added trip to history.
   */
  public UserProfile withAddedTrip(String tripId) {
    List<String> updatedTrips = new ArrayList<>(pastTripIds);
    updatedTrips.add(tripId);
    return new UserProfile(userId, name, email, preferences, updatedTrips);
  }

  /**
   * Returns an unmodifiable view of the preferences.
   */
  public List<TravelPreference> getPreferences() {
    return Collections.unmodifiableList(preferences);
  }

  /**
   * Returns an unmodifiable view of the past trip IDs.
   */
  public List<String> getPastTripIds() {
    return Collections.unmodifiableList(pastTripIds);
  }
}
