package com.travelplanner.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Patch;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import com.travelplanner.application.UserProfileEntity;
import com.travelplanner.domain.TravelPreference;
import com.travelplanner.domain.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * HTTP endpoint for managing user profiles.
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/users")
public class UserProfileEndpoint {

  public record CreateUserRequest(
      String name,
      String email
  ) {
  }

  public record UpdateUserRequest(
      String name,
      String email
  ) {
  }

  public record AddPreferenceRequest(
      TravelPreference.PreferenceType type,
      String value,
      int priority
  ) {
  }

  public record PreferenceResponse(
      String type,
      String value,
      int priority
  ) {
  }

  public record UserProfileResponse(
      String userId,
      String name,
      String email,
      List<PreferenceResponse> preferences,
      List<String> pastTripIds
  ) {
    /**
     * Factory method to create a UserProfileResponse from a UserProfile domain object.
     */
    public static UserProfileResponse fromDomain(UserProfile profile) {
      List<PreferenceResponse> preferenceResponses = profile.preferences().stream()
          .map(pref -> new PreferenceResponse(
              pref.type().name(),
              pref.value(),
              pref.priority()
          ))
          .toList();

      return new UserProfileResponse(
          profile.userId(),
          profile.name(),
          profile.email(),
          preferenceResponses,
          profile.pastTripIds()
      );
    }
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public UserProfileEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  /**
   * Create a new user profile.
   */
  @Post("/{userId}")
  public HttpResponse createUser(String userId, CreateUserRequest request) {
    logger.info("Request to create user: {}", request);

    componentClient.forEventSourcedEntity(userId)
        .method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateUserProfile(
            userId,
            request.name(),
            request.email()
        ));
    return HttpResponses.created();
  }

  /**
   * Get a user profile by ID.
   */
  @Get("/{userId}")
  public UserProfileResponse getUser(String userId) {
    logger.info("Request to get user: {}", userId);

    var userProfile =
        componentClient.forEventSourcedEntity(userId)
            .method(UserProfileEntity::getUserProfile)
            .invoke();
    return UserProfileResponse.fromDomain(userProfile);
  }

  /**
   * Update a user's profile information.
   */
  @Patch("/{userId}")
  public HttpResponse updateUser(String userId, UpdateUserRequest request) {
    logger.info("Request to update user: {}", userId);

    componentClient.forEventSourcedEntity(userId)
        .method(UserProfileEntity::updateUserProfile)
        .invoke(new UserProfileEntity.UpdateUserProfile(
            request.name(),
            request.email()
        ));
    return HttpResponses.ok();
  }

  /**
   * Add a travel preference to a user's profile.
   */
  @Post("/{userId}/preferences")
  public HttpResponse addPreference(String userId, AddPreferenceRequest request) {
    logger.info("Request to add preference for user: {}", userId);

    TravelPreference preference = new TravelPreference(
        request.type(),
        request.value(),
        request.priority()
    );

    componentClient.forEventSourcedEntity(userId)
        .method(UserProfileEntity::addTravelPreference)
        .invoke(new UserProfileEntity.AddTravelPreference(preference));
    return HttpResponses.ok();
  }


}
