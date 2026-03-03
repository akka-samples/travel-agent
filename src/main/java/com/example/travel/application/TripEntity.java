package com.example.travel.application;

import akka.Done;
import akka.javasdk.annotations.Component;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import com.example.travel.domain.TravelPlan;
import com.example.travel.domain.Trip;
import com.example.travel.domain.TripEvent;
import com.example.travel.domain.TripEvent.TripCreated;

import java.time.LocalDate;

@Component(id = "trip")
public class TripEntity extends EventSourcedEntity<Trip, TripEvent> {

  private final String entityId;

  public TripEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Trip emptyState() {
    return null;
  }

  public record CreateTripCommand(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      TravelPlan plan) {
  }

  public Effect<Done> createTrip(CreateTripCommand command) {
    if (currentState() != null) {
      return effects().error("Trip already exists");
    }
    return effects()
        .persist(new TripCreated(
            command.tripId(),
            command.userId(),
            command.destination(),
            command.startDate(),
            command.endDate(),
            command.budget(),
            command.plan()))
        .thenReply(state -> Done.getInstance());
  }

  public ReadOnlyEffect<Trip> getTrip() {
    if (currentState() == null) {
      return effects().error("Trip does not exist");
    }
    return effects().reply(currentState());
  }

  @Override
  public Trip applyEvent(TripEvent event) {
    return switch (event) {
      case TripCreated e -> Trip.create(
          e.tripId(), e.userId(), e.destination(),
          e.startDate(), e.endDate(), e.budget(), e.plan());
    };
  }
}
