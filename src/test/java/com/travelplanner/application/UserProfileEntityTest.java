package com.travelplanner.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedTestKit;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.TravelPreference.PreferenceType;
import com.travelplanner.domain.UserEvent.*;
import com.travelplanner.domain.UserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileEntityTest {

  @Test
  void shouldCreateUserProfile() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    var result = testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("John", "john@example.com"));

    assertThat(result.isReply()).isTrue();
    assertThat(result.getReply()).isEqualTo(Done.getInstance());

    var event = result.getNextEventOfType(UserProfileCreated.class);
    assertThat(event.userId()).isEqualTo("user-1");
    assertThat(event.name()).isEqualTo("John");
    assertThat(event.email()).isEqualTo("john@example.com");

    var state = testKit.getState();
    assertThat(state.name()).isEqualTo("John");
    assertThat(state.email()).isEqualTo("john@example.com");
    assertThat(state.preferences()).isEmpty();
    assertThat(state.pastTripIds()).isEmpty();
  }

  @Test
  void shouldRejectDuplicateCreate() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("John", "john@example.com"));

    var result = testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("Jane", "jane@example.com"));

    assertThat(result.isError()).isTrue();
  }

  @Test
  void shouldRejectEmptyName() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    var result = testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("", "john@example.com"));

    assertThat(result.isError()).isTrue();
  }

  @Test
  void shouldUpdateUserProfile() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("John", "john@example.com"));

    var result = testKit.method(UserProfileEntity::updateUserProfile)
        .invoke(new UserProfileEntity.UpdateCommand("John Updated", "john.updated@example.com"));

    assertThat(result.isReply()).isTrue();

    var state = testKit.getState();
    assertThat(state.name()).isEqualTo("John Updated");
    assertThat(state.email()).isEqualTo("john.updated@example.com");
  }

  @Test
  void shouldAddTravelPreference() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("John", "john@example.com"));

    var preference = new TravelPreference(PreferenceType.ACCOMMODATION_TYPE, "hotel", 5);

    var result = testKit.method(UserProfileEntity::addTravelPreference)
        .invoke(preference);

    assertThat(result.isReply()).isTrue();

    var state = testKit.getState();
    assertThat(state.preferences()).hasSize(1);
    assertThat(state.preferences().getFirst().type()).isEqualTo(PreferenceType.ACCOMMODATION_TYPE);
    assertThat(state.preferences().getFirst().value()).isEqualTo("hotel");
    assertThat(state.preferences().getFirst().priority()).isEqualTo(5);
  }

  @Test
  void shouldAddCompletedTrip() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("John", "john@example.com"));

    var result = testKit.method(UserProfileEntity::addCompletedTrip)
        .invoke("trip-123");

    assertThat(result.isReply()).isTrue();

    var state = testKit.getState();
    assertThat(state.pastTripIds()).containsExactly("trip-123");
  }

  @Test
  void shouldGetUserProfile() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    testKit.method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand("John", "john@example.com"));

    var result = testKit.method(UserProfileEntity::getUserProfile).invoke();

    assertThat(result.isReply()).isTrue();
    var profile = result.getReply();
    assertThat(profile.userId()).isEqualTo("user-1");
    assertThat(profile.name()).isEqualTo("John");
  }

  @Test
  void shouldRejectGetOnNonExistent() {
    var testKit = EventSourcedTestKit.of("user-1", UserProfileEntity::new);

    var result = testKit.method(UserProfileEntity::getUserProfile).invoke();

    assertThat(result.isError()).isTrue();
  }
}
