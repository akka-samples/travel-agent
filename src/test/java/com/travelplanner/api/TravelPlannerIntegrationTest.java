package com.travelplanner.api;

import akka.http.javadsl.model.StatusCodes;
import akka.javasdk.testkit.TestKitSupport;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.Trip;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end integration test. Note that this is making request to the LLM and requires
 * API key to OpenAI in environment variable `OPENAI_API_KEY`.
 */
public class TravelPlannerIntegrationTest extends TestKitSupport {

  private static final Logger logger = LoggerFactory.getLogger(TravelPlannerIntegrationTest.class);
  private final Duration timeout = Duration.of(30, SECONDS);

  @BeforeAll
  @Override
  public void beforeAll() {
    var openAiApiKey = System.getenv("OPENAI_API_KEY");
    if (openAiApiKey == null || openAiApiKey.isEmpty()) {
      throw new RuntimeException("OpenAI API key not found. Set the OPENAI_API_KEY environment variable.");
    } else {
      super.beforeAll();
    }
  }

  @Test
  public void testFullTravelPlanningFlow() {
    // Step 1: Create a user profile
    String userId = "test-user-" + System.currentTimeMillis();
    String userName = "Test Traveler";
    String userEmail = "test@example.com";

    UserProfileEndpoint.CreateUserRequest createUserRequest =
        new UserProfileEndpoint.CreateUserRequest(userName, userEmail);

    var createUserResponse =
        httpClient.POST("/users/" + userId)
            .withRequestBody(createUserRequest)
            .invoke();

    assertThat(createUserResponse.status()).isEqualTo(StatusCodes.CREATED);
    logger.info("Created user profile: {}", userId);

    // Step 2: Add travel preferences to the user
    addTravelPreference(userId, TravelPreference.PreferenceType.ACCOMMODATION_TYPE, "hotel", 5);
    addTravelPreference(userId, TravelPreference.PreferenceType.TRANSPORTATION_TYPE, "train", 4);
    addTravelPreference(userId, TravelPreference.PreferenceType.CUISINE, "french", 5);
    addTravelPreference(userId, TravelPreference.PreferenceType.ACTIVITY, "museums", 5);

    // Step 3: Verify the user profile was created with preferences
    var userProfileResponse =
        httpClient.GET("/users/" + userId)
            .responseBodyAs(UserProfileEndpoint.UserProfileResponse.class)
            .invoke();

    assertThat(userProfileResponse.body().userId()).isEqualTo(userId);
    assertThat(userProfileResponse.body().name()).isEqualTo(userName);
    assertThat(userProfileResponse.body().email()).isEqualTo(userEmail);
    assertThat(userProfileResponse.body().preferences()).hasSize(4);
    logger.info("Verified user profile with preferences");

    // Step 4: Create a travel plan
    LocalDate startDate = LocalDate.now().plusMonths(1);
    LocalDate endDate = startDate.plusDays(7);
    String destination = "Paris, France";
    double budget = 2500.0;

    TravelPlannerEndpoint.GenerateTravelPlanRequest createTripRequest =
        new TravelPlannerEndpoint.GenerateTravelPlanRequest(
            userId, destination, startDate, endDate, budget);

    var createTripResponse =
        httpClient.POST("/travel-planner/create")
            .withRequestBody(createTripRequest)
            .responseBodyAs(TravelPlannerEndpoint.CreateTripResponse.class)
            .invoke();

    String tripId = createTripResponse.body().tripId();
    assertThat(tripId).isNotEmpty();
    logger.info("Created travel plan with ID: {}", tripId);

    // Step 5: Wait for the workflow to complete and the trip to be stored
    // This may take some time as it involves LLM processing
    Awaitility.await()
        .atMost(60, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .ignoreExceptions()
        .until(() -> {
          var tripResponse = httpClient.GET("/travel-planner/trips/" + tripId)
              .responseBodyAs(TravelPlannerEndpoint.TripResponse.class)
              .invoke();

          logger.info("Trip status: {}", tripResponse.body().status());
          return tripResponse.body().status().equals(Trip.TripStatus.PLANNED.name());
        });

    // Step 6: Retrieve and verify the trip details
    var tripResponse =
        httpClient.GET("/travel-planner/trips/" + tripId)
            .responseBodyAs(TravelPlannerEndpoint.TripResponse.class)
            .invoke();

    assertThat(tripResponse.body().tripId()).isEqualTo(tripId);
    assertThat(tripResponse.body().userId()).isEqualTo(userId);
    assertThat(tripResponse.body().destination()).isEqualTo(destination);
    assertThat(tripResponse.body().startDate()).isEqualTo(startDate);
    assertThat(tripResponse.body().endDate()).isEqualTo(endDate);
    assertThat(tripResponse.body().budget()).isEqualTo(budget);

    // Verify the generated plan has content
    assertThat(tripResponse.body().generatedPlan()).isNotNull();
    assertThat(tripResponse.body().generatedPlan().summary()).isNotEmpty();

    int expectedDays = (int) Duration.between(
        startDate.atStartOfDay(),
        endDate.atStartOfDay().plusDays(1)
    ).toDays();
    assertEquals(expectedDays, tripResponse.body().generatedPlan().days().size());

    logger.info("Successfully verified complete travel plan");

    // Step 7: Verify the trip was added to the user's profile
    var updatedUserProfile =
        httpClient.GET("/users/" + userId)
            .responseBodyAs(UserProfileEndpoint.UserProfileResponse.class)
            .invoke();

    assertThat(updatedUserProfile.body().pastTripIds()).contains(tripId);
    logger.info("Verified trip was added to user profile");
  }

  private void addTravelPreference(String userId, TravelPreference.PreferenceType type, String value, int priority) {
    UserProfileEndpoint.AddPreferenceRequest preferenceRequest =
        new UserProfileEndpoint.AddPreferenceRequest(type, value, priority);

    var response =
        httpClient.POST("/users/" + userId + "/preferences")
            .withRequestBody(preferenceRequest)
            .invoke();

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    logger.info("Added preference: {} - {}", type, value);
  }
}
