package com.albertsilva.dev.asjcatalog.factory;

import com.albertsilva.dev.asjcatalog.domain.user.Role;
import com.albertsilva.dev.asjcatalog.dto.role.response.RoleResponse;

public final class RoleFactory {

  public static final Long OPERATOR_ROLE_ID = 1L;
  public static final Long ADMIN_ROLE_ID = 2L;
  public static final Long NON_EXISTING_ROLE_ID = 1000L;
  public static final String OPERATOR_AUTHORITY = "ROLE_OPERATOR";
  public static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

  public static Role createOperatorRole() {
    return new Role(OPERATOR_ROLE_ID, OPERATOR_AUTHORITY);
  }

  public static Role createAdminRole() {
    return new Role(ADMIN_ROLE_ID, ADMIN_AUTHORITY);
  }

  public static RoleResponse createOperatorRoleResponse() {
    return new RoleResponse(OPERATOR_ROLE_ID, OPERATOR_AUTHORITY);
  }

  public static RoleResponse createAdminRoleResponse() {
    return new RoleResponse(ADMIN_ROLE_ID, ADMIN_AUTHORITY);
  }
}