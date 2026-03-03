package com.example.travel.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpException;
import akka.javasdk.http.HttpResponses;
import com.example.travel.application.TravelPlannerWorkflow;
import com.example.travel.application.TripEntity;
import com.example.travel.domain.TravelPlan;
import com.example.travel.domain.Trip;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@HttpEndpoint("/travel-planner")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class TravelPlannerEndpoint {

  private final ComponentClient componentClient;

  public TravelPlannerEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public record CreateTravelPlanRequest(
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget) {
  }

  public record CreateTravelPlanResponse(String tripId) {
  }

  public record TripResponse(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      String status,
      TravelPlan plan) {

    static TripResponse fromDomain(Trip trip) {
      return new TripResponse(
          trip.tripId(), trip.userId(), trip.destination(),
          trip.startDate(), trip.endDate(), trip.budget(),
          trip.status().name(), trip.plan());
    }
  }

  @Post("/create")
  public HttpResponse createTravelPlan(CreateTravelPlanRequest request) {
    if (request.endDate().isBefore(request.startDate())) {
      throw HttpException.badRequest("End date must be after start date");
    }
    if (request.budget() <= 0) {
      throw HttpException.badRequest("Budget must be positive");
    }

    var tripId = UUID.randomUUID().toString();

    componentClient
        .forWorkflow(tripId)
        .method(TravelPlannerWorkflow::createTravelPlan)
        .invoke(new TravelPlannerWorkflow.CreateCommand(
            tripId, request.userId(), request.destination(),
            request.startDate(), request.endDate(), request.budget()));

    return HttpResponses.created(new CreateTravelPlanResponse(tripId));
  }

  @Get("/trips/{tripId}")
  public TripResponse getTrip(String tripId) {
    try {
      var trip = componentClient
          .forEventSourcedEntity(tripId)
          .method(TripEntity::getTrip)
          .invoke();
      return TripResponse.fromDomain(trip);
    } catch (Exception e) {
      throw HttpException.notFound();
    }
  }

  @Get("/trips/{tripId}/as-text")
  public String getTripAsText(String tripId) {
    try {
      var trip = componentClient
          .forEventSourcedEntity(tripId)
          .method(TripEntity::getTrip)
          .invoke();
      return trip.plan().asText(
          trip.destination(),
          trip.startDate().toString(),
          trip.endDate().toString(),
          trip.budget());
    } catch (Exception e) {
      throw HttpException.notFound();
    }
  }
}
