package com.albertsilva.dev.asjcatalog.web.exception.response;

public record FieldMessage(
    String fieldName,
    String message) {
}