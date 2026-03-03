package com.example.travel.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedTestKit;
import com.example.travel.domain.TravelPlan;
import com.example.travel.domain.TripEvent.TripCreated;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripEntityTest {

  private TravelPlan samplePlan() {
    return new TravelPlan(
        "A wonderful trip",
        500.0,
        List.of(new TravelPlan.DayPlan(
            1, "2025-06-15",
            new TravelPlan.Accommodation("Hotel", "Nice hotel", 100.0),
            List.of(new TravelPlan.Transportation("flight", "Airport transfer", 50.0)),
            List.of(new TravelPlan.Activity("Sightseeing", "City tour", 30.0, "morning")),
            List.of(new TravelPlan.Meal("dinner", "Local restaurant", 40.0)),
            220.0)));
  }

  @Test
  void shouldCreateTrip() {
    var testKit = EventSourcedTestKit.of("trip-1", TripEntity::new);

    var command = new TripEntity.CreateTripCommand(
        "trip-1", "user-1", "Paris",
        LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
        2000.0, samplePlan());

    var result = testKit.method(TripEntity::createTrip).invoke(command);

    assertThat(result.isReply()).isTrue();
    assertThat(result.getReply()).isEqualTo(Done.getInstance());

    var event = result.getNextEventOfType(TripCreated.class);
    assertThat(event.tripId()).isEqualTo("trip-1");
    assertThat(event.destination()).isEqualTo("Paris");

    var state = testKit.getState();
    assertThat(state.tripId()).isEqualTo("trip-1");
    assertThat(state.destination()).isEqualTo("Paris");
    assertThat(state.plan().summary()).isEqualTo("A wonderful trip");
    assertThat(state.status()).isEqualTo(com.example.travel.domain.Trip.TripStatus.PLANNED);
  }

  @Test
  void shouldRejectDuplicateCreate() {
    var testKit = EventSourcedTestKit.of("trip-1", TripEntity::new);

    var command = new TripEntity.CreateTripCommand(
        "trip-1", "user-1", "Paris",
        LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
        2000.0, samplePlan());

    testKit.method(TripEntity::createTrip).invoke(command);
    var result = testKit.method(TripEntity::createTrip).invoke(command);

    assertThat(result.isError()).isTrue();
  }

  @Test
  void shouldGetTrip() {
    var testKit = EventSourcedTestKit.of("trip-1", TripEntity::new);

    var command = new TripEntity.CreateTripCommand(
        "trip-1", "user-1", "Paris",
        LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 22),
        2000.0, samplePlan());

    testKit.method(TripEntity::createTrip).invoke(command);
    var result = testKit.method(TripEntity::getTrip).invoke();

    assertThat(result.isReply()).isTrue();
    assertThat(result.getReply().tripId()).isEqualTo("trip-1");
  }

  @Test
  void shouldRejectGetOnNonExistent() {
    var testKit = EventSourcedTestKit.of("trip-1", TripEntity::new);

    var result = testKit.method(TripEntity::getTrip).invoke();

    assertThat(result.isError()).isTrue();
  }
}
