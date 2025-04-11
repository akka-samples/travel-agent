package com.travelplanner.api;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import com.travelplanner.application.TravelPlannerWorkflow;
import com.travelplanner.application.TripEntity;
import com.travelplanner.domain.TravelPlan;
import com.travelplanner.domain.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP endpoint for the travel planner service.
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/travel-planner")
public class TravelPlannerEndpoint {

  public record GenerateTravelPlanRequest(
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget
  ) {
  }

  public record CreateTripResponse(
      String tripId
  ) {
  }

  public record TripResponse(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      TravelPlanResponse generatedPlan,
      String status
  ) {
    /**
     * Factory method to create a TripResponse from a Trip domain object.
     */
    public static TripResponse fromDomain(Trip trip) {
      return new TripResponse(
          trip.tripId(),
          trip.userId(),
          trip.destination(),
          trip.startDate(),
          trip.endDate(),
          trip.budget(),
          TravelPlanResponse.fromDomain(trip.generatedPlan()),
          trip.status().name()
      );
    }
  }

  /**
   * Structured response for a travel plan.
   */
  public record TravelPlanResponse(
      String summary,
      double totalEstimatedCost,
      List<DayPlan> days
  ) {
    public static TravelPlanResponse fromDomain(TravelPlan plan) {
      return new TravelPlanResponse(
          plan.summary(),
          plan.totalEstimatedCost(),
          plan.days().stream().map(DayPlan::fromDomain).toList()
      );
    }

  }

  /**
   * Represents a single day in the travel plan.
   */
  public record DayPlan(
      int dayNumber,
      String date,
      Accommodation accommodation,
      List<Transportation> transportation,
      List<Activity> activities,
      List<Meal> meals,
      double dailyEstimatedCost
  ) {
    public static DayPlan fromDomain(TravelPlan.DayPlan dayPlan) {
      return new DayPlan(
          dayPlan.dayNumber(),
          dayPlan.date(),
          new Accommodation(
              dayPlan.accommodation().name(),
              dayPlan.accommodation().description(),
              dayPlan.accommodation().estimatedCost()
          ),
          dayPlan.transportation().stream()
              .map(t -> new Transportation(
                  t.type(),
                  t.description(),
                  t.estimatedCost()
              ))
              .collect(Collectors.toList()),
          dayPlan.activities().stream()
              .map(a -> new Activity(
                  a.name(),
                  a.description(),
                  a.estimatedCost(),
                  a.timeOfDay()
              ))
              .collect(Collectors.toList()),
          dayPlan.meals().stream()
              .map(m -> new Meal(
                  m.type(),
                  m.suggestion(),
                  m.estimatedCost()
              ))
              .collect(Collectors.toList()),
          dayPlan.dailyEstimatedCost()
      );
    }
  }

  /**
   * Represents accommodation details for a day.
   */
  public record Accommodation(
      String name,
      String description,
      double estimatedCost
  ) {
  }

  /**
   * Represents a transportation segment in the travel plan.
   */
  public record Transportation(
      String type,
      String description,
      double estimatedCost
  ) {
  }

  /**
   * Represents an activity in the travel plan.
   */
  public record Activity(
      String name,
      String description,
      double estimatedCost,
      String timeOfDay
  ) {
  }

  /**
   * Represents a meal recommendation in the travel plan.
   */
  public record Meal(
      String type,
      String suggestion,
      double estimatedCost
  ) {
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public TravelPlannerEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  /**
   * Endpoint to create a travel plan using the workflow.
   * This stores the plan and updates the user profile.
   */
  @Post("/create")
  public CreateTripResponse createTravelPlan(GenerateTravelPlanRequest request) {
    logger.info("Received travel plan creation request for user: {}", request.userId());

    // Generate a unique ID for this travel plan request
    String tripId = UUID.randomUUID().toString();

    logger.info("Creating workflow for trip ID: {}", tripId);

    TravelPlannerWorkflow.CreateTravelPlanCommand command =
        new TravelPlannerWorkflow.CreateTravelPlanCommand(
            request.userId(),
            tripId,
            request.destination(),
            request.startDate(),
            request.endDate(),
            request.budget()
        );

    // Use the tripId as workflowId
    componentClient.forWorkflow(tripId)
        .method(TravelPlannerWorkflow::createTravelPlan)
        .invoke(command);
    return new CreateTripResponse(tripId);
  }

  /**
   * Endpoint to get a trip by ID.
   */
  @Get("/trips/{tripId}")
  public TripResponse getTrip(String tripId) {
    logger.info("Received request to get trip: {}", tripId);

    var trip =
        componentClient.forEventSourcedEntity(tripId)
            .method(TripEntity::getTrip)
            .invoke();
    return TripResponse.fromDomain(trip);
  }

}
