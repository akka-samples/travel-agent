package com.travelplanner.application;

import akka.Done;
import akka.javasdk.annotations.Component;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.UserEvent;
import com.travelplanner.domain.UserEvent.*;
import com.travelplanner.domain.UserProfile;

@Component(id = "user-profile")
public class UserProfileEntity extends EventSourcedEntity<UserProfile, UserEvent> {

  private final String entityId;

  public UserProfileEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public UserProfile emptyState() {
    return null;
  }

  public record CreateCommand(String name, String email) {
  }

  public Effect<Done> createUserProfile(CreateCommand command) {
    if (currentState() != null) {
      return effects().error("User profile already exists");
    }
    if (command.name() == null || command.name().isBlank()) {
      return effects().error("Name must not be empty");
    }
    if (command.email() == null || command.email().isBlank()) {
      return effects().error("Email must not be empty");
    }
    return effects()
        .persist(new UserProfileCreated(entityId, command.name(), command.email()))
        .thenReply(state -> Done.getInstance());
  }

  public record UpdateCommand(String name, String email) {
  }

  public Effect<Done> updateUserProfile(UpdateCommand command) {
    if (currentState() == null) {
      return effects().error("User profile does not exist");
    }
    return effects()
        .persist(new UserProfileUpdated(command.name(), command.email()))
        .thenReply(state -> Done.getInstance());
  }

  public Effect<Done> addTravelPreference(TravelPreference preference) {
    if (currentState() == null) {
      return effects().error("User profile does not exist");
    }
    return effects()
        .persist(new TravelPreferenceAdded(preference))
        .thenReply(state -> Done.getInstance());
  }

  public Effect<Done> addCompletedTrip(String tripId) {
    if (currentState() == null) {
      return effects().error("User profile does not exist");
    }
    return effects()
        .persist(new TripCompleted(tripId))
        .thenReply(state -> Done.getInstance());
  }

  public ReadOnlyEffect<UserProfile> getUserProfile() {
    if (currentState() == null) {
      return effects().error("User profile does not exist");
    }
    return effects().reply(currentState());
  }

  @Override
  public UserProfile applyEvent(UserEvent event) {
    return switch (event) {
      case UserProfileCreated e -> UserProfile.create(e.userId(), e.name(), e.email());
      case UserProfileUpdated e -> currentState().withNameAndEmail(e.name(), e.email());
      case TravelPreferenceAdded e -> currentState().withAddedPreference(e.preference());
      case TripCompleted e -> currentState().withAddedTripId(e.tripId());
    };
  }
}
