package com.travelplanner.domain;

import java.util.ArrayList;
import java.util.List;

public record UserProfile(
    String userId,
    String name,
    String email,
    List<TravelPreference> preferences,
    List<String> pastTripIds) {

  public static UserProfile create(String userId, String name, String email) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name must not be empty");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email must not be empty");
    }
    return new UserProfile(userId, name, email, List.of(), List.of());
  }

  public UserProfile withName(String name) {
    return new UserProfile(userId, name, email, preferences, pastTripIds);
  }

  public UserProfile withEmail(String email) {
    return new UserProfile(userId, name, email, preferences, pastTripIds);
  }

  public UserProfile withNameAndEmail(String name, String email) {
    return new UserProfile(userId, name, email, preferences, pastTripIds);
  }

  public UserProfile withAddedPreference(TravelPreference preference) {
    var updated = new ArrayList<>(preferences);
    updated.add(preference);
    return new UserProfile(userId, name, email, updated, pastTripIds);
  }

  public UserProfile withAddedTripId(String tripId) {
    var updated = new ArrayList<>(pastTripIds);
    updated.add(tripId);
    return new UserProfile(userId, name, email, preferences, updated);
  }
}
