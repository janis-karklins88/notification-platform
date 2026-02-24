package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lv.janis.notification_platform.adminapi.adapter.in.web.dto.ErrorResponse;
import lv.janis.notification_platform.adminapi.application.exception.DuplicateTenantSlugException;

@RestControllerAdvice(basePackages = "lv.janis.notification_platform.adminapi.adapter.in.web")
public class AdminApiExceptionHandler {

  @ExceptionHandler(DuplicateTenantSlugException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateTenantSlug(DuplicateTenantSlugException ex) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(Instant.now(), HttpStatus.CONFLICT.value(), ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
  }
}
