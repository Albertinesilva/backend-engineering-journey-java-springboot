package com.albertsilva.dev.asjcatalog.integrations.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.albertsilva.dev.asjcatalog.utils.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIT {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected TokenUtil tokenUtil;

  protected String bearerToken;

  protected RequestPostProcessor bearerToken() {
    return request -> {
      request.addHeader("Authorization", "Bearer " + bearerToken);
      return request;
    };
  }

  protected String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }
}