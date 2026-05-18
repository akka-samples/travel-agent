package com.travelplanner.domain;

import akka.javasdk.annotations.TypeName;

public sealed interface UserEvent {
  @TypeName("user-profile-created")
  record UserProfileCreated(String userId, String name, String email) implements UserEvent {}

  @TypeName("user-profile-updated")
  record UserProfileUpdated(String name, String email) implements UserEvent {}

  @TypeName("travel-preference-added")
  record TravelPreferenceAdded(TravelPreference preference) implements UserEvent {}

  @TypeName("trip-completed")
  record TripCompleted(String tripId) implements UserEvent {}
}
