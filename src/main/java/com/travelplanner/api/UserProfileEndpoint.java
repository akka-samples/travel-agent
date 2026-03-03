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

import java.util.List;

@HttpEndpoint("/users")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class UserProfileEndpoint {

  private final ComponentClient componentClient;

  public UserProfileEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public record CreateUserRequest(String name, String email) {
  }

  public record UpdateUserRequest(String name, String email) {
  }

  public record AddPreferenceRequest(String type, String value, int priority) {
  }

  public record UserProfileResponse(
      String userId,
      String name,
      String email,
      List<TravelPreference> preferences,
      List<String> pastTripIds) {

    static UserProfileResponse fromDomain(UserProfile profile) {
      return new UserProfileResponse(
          profile.userId(),
          profile.name(),
          profile.email(),
          profile.preferences(),
          profile.pastTripIds());
    }
  }

  @Post("/{userId}")
  public HttpResponse createUser(String userId, CreateUserRequest request) {
    componentClient
        .forEventSourcedEntity(userId)
        .method(UserProfileEntity::createUserProfile)
        .invoke(new UserProfileEntity.CreateCommand(request.name(), request.email()));
    return HttpResponses.created();
  }

  @Get("/{userId}")
  public UserProfileResponse getUser(String userId) {
    var profile = componentClient
        .forEventSourcedEntity(userId)
        .method(UserProfileEntity::getUserProfile)
        .invoke();
    return UserProfileResponse.fromDomain(profile);
  }

  @Patch("/{userId}")
  public HttpResponse updateUser(String userId, UpdateUserRequest request) {
    componentClient
        .forEventSourcedEntity(userId)
        .method(UserProfileEntity::updateUserProfile)
        .invoke(new UserProfileEntity.UpdateCommand(request.name(), request.email()));
    return HttpResponses.ok();
  }

  @Post("/{userId}/preferences")
  public HttpResponse addPreference(String userId, AddPreferenceRequest request) {
    var preferenceType = TravelPreference.PreferenceType.valueOf(request.type());
    var preference = new TravelPreference(preferenceType, request.value(), request.priority());
    componentClient
        .forEventSourcedEntity(userId)
        .method(UserProfileEntity::addTravelPreference)
        .invoke(preference);
    return HttpResponses.ok();
  }
}
