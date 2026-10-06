package com.albertsilva.dev.asjcatalog.factory;

import java.util.Set;

import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.entity.User;

public class UserFactory {

  public static final Long EXISTING_ID = 1L;
  public static final Long NON_EXISTING_ID = 1000L;
  public static final Long COUNT_TOTAL_USERS = 2L;
  public static final String EXISTING_EMAIL = "maria@gmail.com";
  public static final Long ACTIVE_USER_ID = 1L;
  public static final Long INACTIVE_USER_ID = 2L;

  public static User createUser() {
    return new User(null, "João", "Silva", "joao@gmail.com", "JAVA!@#ResTIc18", true);
  }

  public static UserResponse createUserResponse() {
    return new UserResponse(EXISTING_ID, "Maria", "Silva", "maria@gmail.com", Set.of("ROLE_OPERATOR"));
  }

  public static UserDetailsResponse createUserDetailsResponse() {
    return new UserDetailsResponse(EXISTING_ID, "Maria", "Silva", "maria@gmail.com", Set.of(), true);
  }

  public static UserCreateRequest createUserCreateRequest() {
    return new UserCreateRequest("Pedro", "Santos", "pedro@gmail.com", "JAVA!@#ResTIc18", Set.of(1L));
  }

  public static UserUpdateRequest createUserUpdateRequest() {
    return new UserUpdateRequest("Pedro", "Santos Updated", "pedro@gmail.com", "JAVA!@#ResTIc18", Set.of(1L));
  }

  public static UserResponse createUpdatedUserResponse() {
    return new UserResponse(EXISTING_ID, "Pedro", "Santos Updated", "pedro@gmail.com", Set.of("ROLE_OPERATOR"));
  }

}
