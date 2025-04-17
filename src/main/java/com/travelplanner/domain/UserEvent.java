package com.travelplanner.domain;

import akka.javasdk.annotations.TypeName;

/**
 * Events for UserProfileEntity state changes.
 */
public sealed interface UserEvent {

  /**
   * Event emitted when a user profile is created.
   */
  @TypeName("user-profile-created")
  record UserProfileCreated(
      String userId,
      String name,
      String email
  ) implements UserEvent {
  }

  /**
   * Event emitted when a user profile is updated.
   */
  @TypeName("user-profile-updated")
  record UserProfileUpdated(
      String userId,
      String name,
      String email
  ) implements UserEvent {
  }

  /**
   * Event emitted when a travel preference is added to a user profile.
   */
  @TypeName("travel-preference-added")
  record TravelPreferenceAdded(
      String userId,
      TravelPreference preference
  ) implements UserEvent {
  }

  /**
   * Event emitted when a trip is completed and added to user history.
   */
  @TypeName("user-trip-completed")
  record TripCompleted(
      String userId,
      String tripId
  ) implements UserEvent {
  }
}
