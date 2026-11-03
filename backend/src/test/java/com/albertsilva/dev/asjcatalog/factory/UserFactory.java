package com.albertsilva.dev.asjcatalog.factory;

import java.util.Set;

import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;

public class UserFactory {

  public static final Long EXISTING_ID = 1L;
  public static final Long NON_EXISTING_ID = 1000L;
  public static final Long COUNT_TOTAL_USERS = 2L;
  public static final String EXISTING_EMAIL = "maria@gmail.com";
  public static final String NON_EXISTING_EMAIL = "naoexiste@gmail.com";
  public static final Long OPERATOR_USER_ID = 1L;
  public static final String PASSWORD = "JAVA!@#ResTIc18";
  public static final String ENCODED_PASSWORD = "encoded_password";
  public static final String NEW_PASSWORD = "NovaSenha#2026x";
  public static final String ENCODED_NEW_PASSWORD = "encoded_new_password";
  public static final String WRONG_PASSWORD = "SenhaErrada#2026";
  public static final String FIRST_NAME_FILTER = "João";

  public static User createUser() {
    return new User(null, FIRST_NAME_FILTER, "Silva", "joao@gmail.com", PASSWORD, true);
  }

  public static UserResponse createUserResponse() {
    return new UserResponse(EXISTING_ID, "Maria", "Silva", "maria@gmail.com",
        Set.of(RoleFactory.createOperatorRoleResponse()));
  }

  public static UserDetailsResponse createUserDetailsResponse() {
    return new UserDetailsResponse(EXISTING_ID, "Maria", "Silva", "maria@gmail.com", Set.of(), true);
  }

  public static UserCreateRequest createUserCreateRequest() {
    return new UserCreateRequest("Pedro", "Santos", "pedro@gmail.com", PASSWORD,
        Set.of(RoleFactory.OPERATOR_ROLE_ID));
  }

  public static UserUpdateRequest createUserUpdateRequest() {
    return new UserUpdateRequest("Pedro", "Santos Updated", "pedro@gmail.com", PASSWORD,
        Set.of(RoleFactory.OPERATOR_ROLE_ID));
  }

  public static UserRegisterRequest createUserRegisterRequest() {
    return new UserRegisterRequest("Pedro", "Santos", "pedro@gmail.com", PASSWORD);
  }

  public static AuthenticatedUserUpdateRequest createAuthenticatedUserUpdateRequest() {
    return new AuthenticatedUserUpdateRequest("Maria", "Souza", "maria@gmail.com");
  }

  public static UserUpdateRequest createUserUpdateRequestWithoutRolesAndPassword() {
    return new UserUpdateRequest("Pedro", "Santos Updated", "pedro@gmail.com", null, null);
  }

  public static UserResponse createUpdatedUserResponse() {
    return new UserResponse(EXISTING_ID, "Pedro", "Santos Updated", "pedro@gmail.com",
        Set.of(RoleFactory.createAdminRoleResponse()));
  }

}
