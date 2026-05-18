package com.travelplanner.application;

import static java.time.Duration.ofSeconds;

import akka.Done;
import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.StepName;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import com.travelplanner.domain.TravelPlan;
import java.time.LocalDate;

@Component(id = "travel-planner-workflow")
public class TravelPlannerWorkflow extends Workflow<TravelPlannerWorkflow.State> {

  public enum WorkflowStatus {
    STARTED,
    PLAN_GENERATED,
    TRIP_STORED,
    COMPLETED,
    ERROR,
  }

  public record State(
    String tripId,
    String userId,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    double budget,
    WorkflowStatus status,
    TravelPlan generatedPlan
  ) {
    State withStatus(WorkflowStatus status) {
      return new State(
        tripId,
        userId,
        destination,
        startDate,
        endDate,
        budget,
        status,
        generatedPlan
      );
    }

    State withGeneratedPlan(TravelPlan plan) {
      return new State(
        tripId,
        userId,
        destination,
        startDate,
        endDate,
        budget,
        WorkflowStatus.PLAN_GENERATED,
        plan
      );
    }
  }

  public record CreateCommand(
    String tripId,
    String userId,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    double budget
  ) {}

  private final ComponentClient componentClient;

  public TravelPlannerWorkflow(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowSettings settings() {
    return WorkflowSettings.builder()
      .defaultStepTimeout(ofSeconds(120))
      .defaultStepRecovery(maxRetries(2).failoverTo(TravelPlannerWorkflow::errorStep))
      .build();
  }

  public Effect<Done> createTravelPlan(CreateCommand command) {
    if (currentState() != null) {
      return effects().error("Workflow already started");
    }
    var initialState = new State(
      command.tripId(),
      command.userId(),
      command.destination(),
      command.startDate(),
      command.endDate(),
      command.budget(),
      WorkflowStatus.STARTED,
      null
    );

    return effects()
      .updateState(initialState)
      .transitionTo(TravelPlannerWorkflow::generatePlan)
      .thenReply(Done.getInstance());
  }

  public ReadOnlyEffect<State> getStatus() {
    if (currentState() == null) {
      return effects().error("Workflow not started");
    }
    return effects().reply(currentState());
  }

  @StepName("generate-plan")
  private StepEffect generatePlan() {
    var request = new TravelPlannerAgent.GenerateRequest(
      currentState().userId(),
      currentState().destination(),
      currentState().startDate(),
      currentState().endDate(),
      currentState().budget()
    );

    var plan = componentClient
      .forAgent()
      .inSession(sessionId())
      .method(TravelPlannerAgent::generateTravelPlan)
      .invoke(request);

    return stepEffects()
      .updateState(currentState().withGeneratedPlan(plan))
      .thenTransitionTo(TravelPlannerWorkflow::storeTrip);
  }

  @StepName("store-trip")
  private StepEffect storeTrip() {
    var command = new TripEntity.CreateTripCommand(
      currentState().tripId(),
      currentState().userId(),
      currentState().destination(),
      currentState().startDate(),
      currentState().endDate(),
      currentState().budget(),
      currentState().generatedPlan()
    );

    componentClient
      .forEventSourcedEntity(currentState().tripId())
      .method(TripEntity::createTrip)
      .invoke(command);

    return stepEffects()
      .updateState(currentState().withStatus(WorkflowStatus.TRIP_STORED))
      .thenTransitionTo(TravelPlannerWorkflow::updateUserProfile);
  }

  @StepName("update-user-profile")
  private StepEffect updateUserProfile() {
    componentClient
      .forEventSourcedEntity(currentState().userId())
      .method(UserProfileEntity::addCompletedTrip)
      .invoke(currentState().tripId());

    return stepEffects()
      .updateState(currentState().withStatus(WorkflowStatus.COMPLETED))
      .thenEnd();
  }

  @StepName("error")
  private StepEffect errorStep() {
    return stepEffects()
      .updateState(currentState().withStatus(WorkflowStatus.ERROR))
      .thenEnd();
  }

  private String sessionId() {
    return commandContext().workflowId();
  }
}
