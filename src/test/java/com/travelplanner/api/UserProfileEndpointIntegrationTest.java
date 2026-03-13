package com.travelplanner.api;

import static org.assertj.core.api.Assertions.assertThat;

import akka.javasdk.testkit.TestKitSupport;
import com.travelplanner.api.UserProfileEndpoint.AddPreferenceRequest;
import com.travelplanner.api.UserProfileEndpoint.CreateUserRequest;
import com.travelplanner.api.UserProfileEndpoint.UpdateUserRequest;
import com.travelplanner.api.UserProfileEndpoint.UserProfileResponse;
import org.junit.jupiter.api.Test;

class UserProfileEndpointIntegrationTest extends TestKitSupport {

  @Test
  void shouldCreateAndGetUser() {
    var userId = "test-user-" + System.nanoTime();

    var createResponse = httpClient
      .POST("/users/" + userId)
      .withRequestBody(new CreateUserRequest("John", "john@example.com"))
      .invoke();
    assertThat(createResponse.status().intValue()).isEqualTo(201);

    var getResponse = httpClient
      .GET("/users/" + userId)
      .responseBodyAs(UserProfileResponse.class)
      .invoke();
    assertThat(getResponse.status().isSuccess()).isTrue();
    assertThat(getResponse.body().name()).isEqualTo("John");
    assertThat(getResponse.body().email()).isEqualTo("john@example.com");
    assertThat(getResponse.body().preferences()).isEmpty();
    assertThat(getResponse.body().pastTripIds()).isEmpty();
  }

  @Test
  void shouldUpdateUser() {
    var userId = "test-user-" + System.nanoTime();

    httpClient
      .POST("/users/" + userId)
      .withRequestBody(new CreateUserRequest("John", "john@example.com"))
      .invoke();

    var updateResponse = httpClient
      .PATCH("/users/" + userId)
      .withRequestBody(new UpdateUserRequest("John Updated", "john.updated@example.com"))
      .invoke();
    assertThat(updateResponse.status().isSuccess()).isTrue();

    var getResponse = httpClient
      .GET("/users/" + userId)
      .responseBodyAs(UserProfileResponse.class)
      .invoke();
    assertThat(getResponse.body().name()).isEqualTo("John Updated");
    assertThat(getResponse.body().email()).isEqualTo("john.updated@example.com");
  }

  @Test
  void shouldAddPreference() {
    var userId = "test-user-" + System.nanoTime();

    httpClient
      .POST("/users/" + userId)
      .withRequestBody(new CreateUserRequest("John", "john@example.com"))
      .invoke();

    var prefResponse = httpClient
      .POST("/users/" + userId + "/preferences")
      .withRequestBody(new AddPreferenceRequest("ACCOMMODATION_TYPE", "hotel", 5))
      .invoke();
    assertThat(prefResponse.status().isSuccess()).isTrue();

    var getResponse = httpClient
      .GET("/users/" + userId)
      .responseBodyAs(UserProfileResponse.class)
      .invoke();
    assertThat(getResponse.body().preferences()).hasSize(1);
    assertThat(getResponse.body().preferences().getFirst().value()).isEqualTo("hotel");
  }
}
