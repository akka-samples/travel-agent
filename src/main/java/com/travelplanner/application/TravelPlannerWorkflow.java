package com.travelplanner.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import com.travelplanner.domain.TravelPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;

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
      ERROR
    }

    /**
     * Returns a new state with updated status.
     */
    public State withStatus(Status newStatus) {
      return new State(
          tripId,
          userId,
          destination,
          startDate,
          endDate,
          budget,
          newStatus
      );
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
  public WorkflowDef<State> definition() {
    return workflow()
        .addStep(generatePlan())
        .addStep(storeTrip())
        .addStep(updateUserProfile())
        .addStep(errorStep())
        .defaultStepRecoverStrategy(maxRetries(2).failoverTo("error"));
  }

  // Step 1: Generate the travel plan
  private Step generatePlan() {
    return step("generate-plan")
        .call(() ->
            componentClient.forAgent().inSession(sessionId())
                .method(TravelPlannerAgent::generateTravelPlan)
                .invoke(new TravelPlannerAgent.Request(
                    currentState().userId(),
                    currentState().destination(),
                    currentState().startDate(),
                    currentState().endDate(),
                    currentState().budget()))
        )
        .andThen(TravelPlan.class, generatedPlan -> {
          logger.info("Generated travel plan for trip {}", currentState().tripId());

          return effects()
              .updateState(currentState().withStatus(State.Status.PLAN_GENERATED))
              .transitionTo("store-trip", generatedPlan);
        })
        .timeout(Duration.ofSeconds(120));
  }

  // Step 2: Store the trip in the TripEntity
  private Step storeTrip() {
    return step("store-trip")
        .call(TravelPlan.class, generatedPlan -> {
          TripEntity.CreateTrip createTripCmd = new TripEntity.CreateTrip(
              currentState().tripId(),
              currentState().userId(),
              currentState().destination(),
              currentState().startDate(),
              currentState().endDate(),
              currentState().budget(),
              generatedPlan
          );

          return
              componentClient.forEventSourcedEntity(currentState().tripId())
                  .method(TripEntity::createTrip)
                  .invoke(createTripCmd);
        })
        .andThen(() -> {
          logger.info("Stored trip {}", currentState().tripId());

          return effects()
              .updateState(currentState().withStatus(State.Status.TRIP_STORED))
              .transitionTo("update-user-profile");
        });
  }

  // Step 3: Update the user profile
  private Step updateUserProfile() {
    return step("update-user-profile")
        .call(() -> {
          UserProfileEntity.AddCompletedTrip addTripCmd =
              new UserProfileEntity.AddCompletedTrip(currentState().tripId());

          return
              componentClient.forEventSourcedEntity(currentState().userId())
                  .method(UserProfileEntity::addCompletedTrip)
                  .invoke(addTripCmd);
        })
        .andThen(() -> {
          logger.info("Updated user profile for trip {}", currentState().tripId());

          return effects()
              .updateState(currentState().withStatus(State.Status.COMPLETED))
              .end();
        });
  }

  private Step errorStep() {
    return step("error")
        .call(() -> {
              logger.error("Workflow for trip [{}] failed", currentState().tripId());
              return Done.done();
            }
        )
        .andThen(() -> effects()
              .updateState(currentState().withStatus(State.Status.ERROR))
              .end());
  }

  /**
   * Starts the travel planning workflow.
   */
  public Effect<Done> createTravelPlan(CreateTravelPlanCommand cmd) {
    if (currentState() != null) {
      return effects().error("Workflow already started");
    }

    logger.info("Starting travel plan workflow for user {} to {}",
        cmd.userId(), cmd.destination());

    // Create initial state
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
        .transitionTo("generate-plan")
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
