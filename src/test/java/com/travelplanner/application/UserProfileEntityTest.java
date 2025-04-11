package com.travelplanner.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.UserEvent;
import com.travelplanner.domain.UserProfile;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserProfileEntityTest {

  @Test
  public void testCreateUserProfile() {
    var testKit = EventSourcedTestKit.of(__ -> new UserProfileEntity());

    var userId = "user-123";
    var name = "John Traveler";
    var email = "john@example.com";

    var result = testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateUserProfile(userId, name, email));

    // Verify the result
    assertThat(result.getReply()).isEqualTo(Done.done());

    // Verify the event
    var event = result.getNextEventOfType(UserEvent.UserProfileCreated.class);
    assertThat(event.userId()).isEqualTo(userId);
    assertThat(event.name()).isEqualTo(name);
    assertThat(event.email()).isEqualTo(email);

    // Verify state
    var state = testKit.method(UserProfileEntity::getUserProfile).invoke();
    assertThat(state.getReply().userId()).isEqualTo(userId);
    assertThat(state.getReply().name()).isEqualTo(name);
    assertThat(state.getReply().email()).isEqualTo(email);
  }

  @Test
  public void testUpdateUserProfile() {
    var testKit = EventSourcedTestKit.of(__ -> new UserProfileEntity());

    // Create user first
    var userId = "user-123";
    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateUserProfile(userId, "John Traveler", "john@example.com"));

    // Update the profile
    var newName = "John Updated";
    var newEmail = "john.updated@example.com";

    var result = testKit.method(UserProfileEntity::updateUserProfile)
        .invoke(new UserProfileEntity.UpdateUserProfile(newName, newEmail));

    // Verify the result
    assertThat(result.getReply()).isEqualTo(Done.done());

    // Verify the event
    var event = result.getNextEventOfType(UserEvent.UserProfileUpdated.class);
    assertThat(event.name()).isEqualTo(newName);
    assertThat(event.email()).isEqualTo(newEmail);

    // Verify state
    var state = testKit.method(UserProfileEntity::getUserProfile).invoke();
    assertThat(state.getReply().name()).isEqualTo(newName);
    assertThat(state.getReply().email()).isEqualTo(newEmail);
  }

  @Test
  public void testAddTravelPreference() {
    var testKit = EventSourcedTestKit.of(__ -> new UserProfileEntity());

    // Create user first
    var userId = "user-123";
    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateUserProfile(userId, "John Traveler", "john@example.com"));

    // Add a travel preference
    var preference = new TravelPreference(
        TravelPreference.PreferenceType.ACCOMMODATION_TYPE,
        "hotel",
        5
    );

    var result = testKit.method(UserProfileEntity::addTravelPreference)
        .invoke(new UserProfileEntity.AddTravelPreference(preference));

    // Verify the event
    var event = result.getNextEventOfType(UserEvent.TravelPreferenceAdded.class);
    assertThat(event.preference()).isEqualTo(preference);

    // Verify state
    var state = testKit.method(UserProfileEntity::getUserProfile).invoke();
    assertThat(state.getReply().preferences().size()).isEqualTo(1);
    assertThat(state.getReply().preferences().get(0)).isEqualTo(preference);
  }

  @Test
  public void testAddCompletedTrip() {
    var testKit = EventSourcedTestKit.of(__ -> new UserProfileEntity());

    // Create user first
    var userId = "user-123";
    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateUserProfile(userId, "John Traveler", "john@example.com"));

    // Add a completed trip
    var tripId = "trip-456";

    var result = testKit.method(UserProfileEntity::addCompletedTrip)
        .invoke(new UserProfileEntity.AddCompletedTrip(tripId));

    // Verify the event
    var event = result.getNextEventOfType(UserEvent.TripCompleted.class);
    assertThat(event.tripId()).isEqualTo(tripId);

    // Verify state
    var state = testKit.method(UserProfileEntity::getUserProfile).invoke();
    assertThat(state.getReply().pastTripIds().size()).isEqualTo(1);
    assertThat(state.getReply().pastTripIds().get(0)).isEqualTo(tripId);
  }

  @Test
  public void testInvalidCommands() {
    var testKit = EventSourcedTestKit.of(__ -> new UserProfileEntity());

    // Test empty name
    var result1 = testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateUserProfile("user-123", "", "john@example.com"));
    assertThat(result1.isError()).isTrue();

    // Test empty email
    var result2 = testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateUserProfile("user-123", "John", ""));
    assertThat(result2.isError()).isTrue();

    // Test operations on non-existent user
    var result3 = testKit.method(UserProfileEntity::updateUserProfile)
        .invoke(new UserProfileEntity.UpdateUserProfile("John Updated", "john@example.com"));
    assertThat(result3.isError()).isTrue();

    var result4 = testKit.method(UserProfileEntity::addTravelPreference)
        .invoke(new UserProfileEntity.AddTravelPreference(
            new TravelPreference(TravelPreference.PreferenceType.ACCOMMODATION_TYPE, "hotel", 5)
        ));
    assertThat(result4.isError()).isTrue();
  }
}
