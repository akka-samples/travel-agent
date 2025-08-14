package com.travelplanner.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.StepName;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import com.travelplanner.domain.TravelPlan;
import java.time.Duration;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow that coordinates the travel planning process.
 */
@ComponentId("travel-planner-workflow")
public class TravelPlannerWorkflow extends Workflow<TravelPlannerWorkflow.State> {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  /**
   * State for the TravelPlannerWorkflow.
   */
  public record State(
      String tripId,
      String userId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget,
      Status status
  ) {
    /**
     * Status of the travel planning workflow.
     */
    public enum Status {
      STARTED,
      PLAN_GENERATED,
      TRIP_STORED,
      COMPLETED,
      ERROR,
    }

    /**
     * Returns a new state with updated status.
     */
    public State withStatus(Status newStatus) {
      return new State(tripId, userId, destination, startDate, endDate, budget, newStatus);
    }
  }

  public TravelPlannerWorkflow(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  /**
   * Command to create a travel plan.
   */
  public record CreateTravelPlanCommand(
      String userId,
      String tripId,
      String destination,
      LocalDate startDate,
      LocalDate endDate,
      double budget
  ) {
  }

  @Override
  public WorkflowSettings settings() {
    return WorkflowSettings.builder()
      .defaultStepRecovery(maxRetries(2).failoverTo(TravelPlannerWorkflow::errorStep))
      .stepTimeout(TravelPlannerWorkflow::generatePlan, Duration.ofSeconds(120))
      .build();
  }

  // Step 1: Generate the travel plan
  @StepName("generate-plan")
  private StepEffect generatePlan() {
    var generatedPlan = componentClient
      .forAgent()
      .inSession(sessionId())
      .method(TravelPlannerAgent::generateTravelPlan)
      .invoke(
        new TravelPlannerAgent.Request(
          currentState().userId(),
          currentState().destination(),
          currentState().startDate(),
          currentState().endDate(),
          currentState().budget()
        )
      );

    logger.info("Generated travel plan for trip {}", currentState().tripId());
    return stepEffects()
      .updateState(currentState().withStatus(State.Status.PLAN_GENERATED))
      .thenTransitionTo(TravelPlannerWorkflow::storeTrip)
      .withInput(generatedPlan);
  }

  // Step 2: Store the trip in the TripEntity
  @StepName("store-trip")
  private StepEffect storeTrip(TravelPlan generatedPlan) {

    TripEntity.CreateTrip createTripCmd = new TripEntity.CreateTrip(
      currentState().tripId(),
      currentState().userId(),
      currentState().destination(),
      currentState().startDate(),
      currentState().endDate(),
      currentState().budget(),
      generatedPlan
    );

    componentClient
      .forEventSourcedEntity(currentState().tripId())
      .method(TripEntity::createTrip)
      .invoke(createTripCmd);

    logger.info("Stored trip {}", currentState().tripId());

    return stepEffects()
      .updateState(currentState().withStatus(State.Status.TRIP_STORED))
      .thenTransitionTo(TravelPlannerWorkflow::updateUserProfile);
  }

  // Step 3: Update the user profile
  @StepName("update-user-profile")
  private StepEffect updateUserProfile() {

    UserProfileEntity.AddCompletedTrip addTripCmd =
      new UserProfileEntity.AddCompletedTrip(currentState().tripId());

    componentClient
      .forEventSourcedEntity(currentState().userId())
      .method(UserProfileEntity::addCompletedTrip)
      .invoke(addTripCmd);

    logger.info("Updated user profile for trip {}", currentState().tripId());

    return stepEffects()
      .updateState(currentState().withStatus(State.Status.COMPLETED))
      .thenEnd();
  }

  @StepName("error")
  private StepEffect errorStep() {
    logger.error("Workflow for trip [{}] failed", currentState().tripId());
    return stepEffects().updateState(currentState().withStatus(State.Status.ERROR)).thenEnd();
  }

  /**
   * Starts the travel planning workflow.
   */
  public Effect<Done> createTravelPlan(CreateTravelPlanCommand cmd) {

    if (currentState() != null) {
      return effects().error("Workflow already started");
    }

    logger.info(
      "Starting travel plan workflow for user {} to {}",
      cmd.userId(),
      cmd.destination()
    );

    // Create an initial state
    State initialState = new State(
      cmd.tripId(),
      cmd.userId(),
      cmd.destination(),
      cmd.startDate(),
      cmd.endDate(),
      cmd.budget(),
      State.Status.STARTED
    );

    return effects()
      .updateState(initialState)
      .transitionTo(TravelPlannerWorkflow::generatePlan)
      .thenReply(Done.getInstance());
  }

  /**
   * Gets the current state of the travel planning workflow.
   */
  public ReadOnlyEffect<State> getWorkflowState() {
    if (currentState() == null) {
      return effects().error("Workflow not started");
    }
    return effects().reply(currentState());
  }

  private String sessionId() {
    return commandContext().workflowId();
  }
}
