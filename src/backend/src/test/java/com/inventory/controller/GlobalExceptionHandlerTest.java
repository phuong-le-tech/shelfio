package com.inventory.controller;

import com.inventory.dto.response.ApiErrorResponse;
import com.inventory.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        Environment env = mock(Environment.class);
        handler = new GlobalExceptionHandler(env);
    }

    @Test
    @DisplayName("ItemNotFoundException → 404")
    void itemNotFound_returns404() {
        UUID id = UUID.randomUUID();
        ResponseEntity<ApiErrorResponse> response = handler.handleItemNotFound(
                new ItemNotFoundException(id));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(404);
    }

    @Test
    @DisplayName("ItemListNotFoundException → 404")
    void itemListNotFound_returns404() {
        UUID id = UUID.randomUUID();
        ResponseEntity<ApiErrorResponse> response = handler.handleItemListNotFound(
                new ItemListNotFoundException(id));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo(404);
    }

    @Test
    @DisplayName("UserNotFoundException → 404")
    void userNotFound_returns404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUserNotFound(
                new UserNotFoundException("not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UserAlreadyExistsException → 409")
    void userAlreadyExists_returns409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUserAlreadyExists(
                new UserAlreadyExistsException("exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error().code()).isEqualTo(409);
    }

    @Test
    @DisplayName("CustomFieldValidationException → 400")
    void customFieldValidation_returns400() {
        ResponseEntity<ApiErrorResponse> response = handler.handleCustomFieldValidation(
                new CustomFieldValidationException("invalid field"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().message()).isEqualTo("invalid field");
    }

    @Test
    @DisplayName("FileValidationException → 400")
    void fileValidation_returns400() {
        ResponseEntity<ApiErrorResponse> response = handler.handleFileValidation(
                new FileValidationException("bad file"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("UnauthorizedException → 401")
    void unauthorized_returns401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("not authenticated"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error().code()).isEqualTo(401);
    }

    @Test
    @DisplayName("AccessDeniedException → 403")
    void accessDenied_returns403() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("BadCredentialsException → 401")
    void badCredentials_returns401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBadCredentials(
                new BadCredentialsException("wrong password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error().message()).isEqualTo("Invalid email or password");
    }

    @Test
    @DisplayName("RateLimitExceededException → 429")
    void rateLimit_returns429() {
        ResponseEntity<ApiErrorResponse> response = handler.handleRateLimit(
                new RateLimitExceededException("too many requests"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().error().code()).isEqualTo(429);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("ConstraintViolationException → 400")
    void constraintViolation_returns400() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiErrorResponse> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().message()).contains("must not be blank");
    }

    @Test
    @DisplayName("Generic Exception → 500 (non-dev mode)")
    void genericException_returns500() {
        ResponseEntity<ApiErrorResponse> response = handler.handleException(
                new RuntimeException("something broke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().message()).isEqualTo("An unexpected error occurred");
    }
}
