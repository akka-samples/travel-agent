package com.travelplanner.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import com.travelplanner.domain.TravelPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelplanner.domain.Trip;
import com.travelplanner.domain.TripEvent;

import java.time.LocalDate;

import static akka.Done.done;

/**
 * Entity for managing trips in the travel planner service.
 */
@ComponentId("trip")
public class TripEntity extends EventSourcedEntity<Trip, TripEvent> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  // Command records
  public record CreateTrip(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      TravelPlan generatedPlan
  ) {
  }

  public record UpdateTripPlan(
      TravelPlan generatedPlan
  ) {
  }

  public record MarkTripAsBooked() {
  }

  public record MarkTripAsCompleted() {
  }

  /**
   * Creates a new trip with a generated plan.
   */
  public Effect<Done> createTrip(CreateTrip cmd) {
    if (currentState() != null) {
      logger.info("Trip already exists: {}", cmd.tripId());
      return effects().reply(done());
    }

    logger.info("Creating trip: {}", cmd.tripId());
    return effects()
        .persist(new TripEvent.TripCreated(
            cmd.tripId(),
            cmd.userId(),
            cmd.destination(),
            cmd.startDate(),
            cmd.endDate(),
            cmd.budget(),
            cmd.generatedPlan()
        ))
        .thenReply(__ -> done());
  }

  /**
   * Updates the generated plan for a trip.
   */
  public Effect<Done> updateTripPlan(UpdateTripPlan cmd) {
    if (currentState() == null) {
      return effects().error("Trip not found");
    }

    logger.info("Updating trip plan: {}", currentState().tripId());
    return effects()
        .persist(new TripEvent.TripPlanUpdated(
            currentState().tripId(),
            cmd.generatedPlan()
        ))
        .thenReply(__ -> done());
  }

  /**
   * Marks a trip as booked.
   */
  public Effect<Done> markTripAsBooked(MarkTripAsBooked cmd) {
    if (currentState() == null) {
      return effects().error("Trip not found");
    }

    if (currentState().status() == Trip.TripStatus.BOOKED) {
      logger.info("Trip already booked: {}", currentState().tripId());
      return effects().reply(done());
    }

    logger.info("Marking trip as booked: {}", currentState().tripId());
    return effects()
        .persist(new TripEvent.TripBooked(currentState().tripId()))
        .thenReply(__ -> done());
  }

  /**
   * Marks a trip as completed.
   */
  public Effect<Done> markTripAsCompleted(MarkTripAsCompleted cmd) {
    if (currentState() == null) {
      return effects().error("Trip not found");
    }

    if (currentState().status() == Trip.TripStatus.COMPLETED) {
      logger.info("Trip already completed: {}", currentState().tripId());
      return effects().reply(done());
    }

    logger.info("Marking trip as completed: {}", currentState().tripId());
    return effects()
        .persist(new TripEvent.TripCompleted(currentState().tripId()))
        .thenReply(__ -> done());
  }

  /**
   * Gets the current trip.
   */
  public ReadOnlyEffect<Trip> getTrip() {
    if (currentState() == null) {
      return effects().error("Trip not found");
    }
    return effects().reply(currentState());
  }

  @Override
  public Trip applyEvent(TripEvent event) {
    return switch (event) {
      case TripEvent.TripCreated evt -> Trip.create(
          evt.tripId(),
          evt.userId(),
          evt.destination(),
          evt.startDate(),
          evt.endDate(),
          evt.budget(),
          evt.generatedPlan()
      );

      case TripEvent.TripPlanUpdated evt -> currentState().withUpdatedPlan(evt.generatedPlan());

      case TripEvent.TripBooked evt -> currentState().withStatus(Trip.TripStatus.BOOKED);

      case TripEvent.TripCompleted evt -> currentState().withStatus(Trip.TripStatus.COMPLETED);
    };
  }
}
