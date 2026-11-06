package com.albertsilva.dev.asjcatalog.factory;

import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_ROLE_ID;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.OPERATOR_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.OPERATOR_ROLE_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ENCODED_PASSWORD;

import java.util.List;

import com.albertsilva.dev.asjcatalog.projection.UserDetailsProjection;

public class UserDetailsFactory {

  public static List<UserDetailsProjection> createOperatorProjections(Long id, String username, boolean active) {
    return List.of(new UserDetailsImpl(id, username, ENCODED_PASSWORD, OPERATOR_ROLE_ID, OPERATOR_AUTHORITY, active));
  }

  public static List<UserDetailsProjection> createOperatorAndAdminProjections(Long id, String username) {
    return List.of(
        new UserDetailsImpl(id, username, ENCODED_PASSWORD, OPERATOR_ROLE_ID, OPERATOR_AUTHORITY, true),
        new UserDetailsImpl(id, username, ENCODED_PASSWORD, ADMIN_ROLE_ID, ADMIN_AUTHORITY, true));
  }
}

class UserDetailsImpl implements UserDetailsProjection {

  private Long id;
  private String username;
  private String password;
  private Long roleId;
  private String authority;
  private boolean active;

  public UserDetailsImpl(Long id, String username, String password, Long roleId, String authority, boolean active) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.roleId = roleId;
    this.authority = authority;
    this.active = active;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public Long getRoleId() {
    return roleId;
  }

  @Override
  public String getAuthority() {
    return authority;
  }

  @Override
  public boolean getActive() {
    return active;
  }

}