package com.albertsilva.dev.dscatalog.security.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException;

@Service
public class AuthenticatedUserService {

  private final UserRepository userRepository;

  public AuthenticatedUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User getAuthenticatedUser() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      throw new AuthenticatedUserNotFoundException("error.auth.authentication.notFound");
    }

    if (!(authentication.getPrincipal() instanceof Jwt)) {
      throw new AuthenticatedUserNotFoundException("error.auth.invalid.principal");
    }

    Jwt jwt = (Jwt) authentication.getPrincipal();

    String username = jwt.getClaimAsString("username");

    if (username == null || username.isBlank()) {
      throw new AuthenticatedUserNotFoundException("error.auth.username.claim.notFound");
    }

    return userRepository.findByEmail(username)
        .orElseThrow(() -> new AuthenticatedUserNotFoundException("error.auth.user.notFound"));
  }
}
