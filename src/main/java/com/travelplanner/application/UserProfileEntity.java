package com.travelplanner.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelplanner.domain.UserProfile;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.UserEvent;

import static akka.Done.done;

/**
 * Entity for managing user profiles in the travel planner service.
 */
@ComponentId("user-profile")
public class UserProfileEntity extends EventSourcedEntity<UserProfile, UserEvent> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  // Command records defined within the entity
  public record CreateUserProfile(
      String userId,
      String name,
      String email
  ) {
  }

  public record UpdateUserProfile(
      String name,
      String email
  ) {
  }

  public record AddTravelPreference(
      TravelPreference preference
  ) {
  }

  public record AddCompletedTrip(
      String tripId
  ) {
  }

  /**
   * Creates a new user profile.
   */
  public Effect<Done> createUserProfile(CreateUserProfile cmd) {
    if (cmd.name() == null || cmd.name().isBlank()) {
      return effects().error("Name cannot be empty");
    }

    if (cmd.email() == null || cmd.email().isBlank()) {
      return effects().error("Email cannot be empty");
    }

    if (currentState() != null) {
      logger.info("User profile already exists: {}", cmd.userId());
      return effects().reply(done());
    }

    logger.info("Creating user profile: {}", cmd);
    return effects()
        .persist(new UserEvent.UserProfileCreated(cmd.userId(), cmd.name(), cmd.email()))
        .thenReply(__ -> done());
  }

  /**
   * Updates an existing user profile.
   */
  public Effect<Done> updateUserProfile(UpdateUserProfile cmd) {
    if (currentState() == null) {
      return effects().error("User profile not found");
    }

    if (cmd.name() == null || cmd.name().isBlank()) {
      return effects().error("Name cannot be empty");
    }

    if (cmd.email() == null || cmd.email().isBlank()) {
      return effects().error("Email cannot be empty");
    }

    logger.info("Updating user profile: {}", cmd);
    return effects()
        .persist(new UserEvent.UserProfileUpdated(currentState().userId(), cmd.name(), cmd.email()))
        .thenReply(__ -> done());
  }

  /**
   * Adds a travel preference to the user profile.
   */
  public Effect<Done> addTravelPreference(AddTravelPreference cmd) {
    if (currentState() == null) {
      return effects().error("User profile not found");
    }

    logger.info("Adding travel preference: {}", cmd.preference());
    return effects()
        .persist(new UserEvent.TravelPreferenceAdded(currentState().userId(), cmd.preference()))
        .thenReply(__ -> done());
  }

  /**
   * Adds a completed trip to the user's history.
   */
  public Effect<Done> addCompletedTrip(AddCompletedTrip cmd) {
    if (currentState() == null) {
      return effects().error("User profile not found");
    }

    logger.info("Adding completed trip: {}", cmd.tripId());
    return effects()
        .persist(new UserEvent.TripCompleted(currentState().userId(), cmd.tripId()))
        .thenReply(__ -> done());
  }

  /**
   * Gets the current user profile.
   */
  public ReadOnlyEffect<UserProfile> getUserProfile() {
    if (currentState() == null) {
      return effects().error("User profile not found");
    }
    return effects().reply(currentState());
  }

  @Override
  public UserProfile applyEvent(UserEvent event) {
    return switch (event) {
      case UserEvent.UserProfileCreated evt -> UserProfile.create(evt.userId(), evt.name(), evt.email());

      case UserEvent.UserProfileUpdated evt -> currentState().withNameAndEmail(evt.name(), evt.email());

      case UserEvent.TravelPreferenceAdded evt -> currentState().withAddedPreference(evt.preference());

      case UserEvent.TripCompleted evt -> currentState().withAddedTrip(evt.tripId());
    };
  }
}
