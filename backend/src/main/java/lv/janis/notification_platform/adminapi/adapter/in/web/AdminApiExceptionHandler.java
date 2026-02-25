package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lv.janis.notification_platform.adminapi.adapter.in.web.dto.ErrorResponse;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.ConflictException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;

@RestControllerAdvice(basePackages = "lv.janis.notification_platform.adminapi.adapter.in.web")
public class AdminApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(Instant.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(Instant.now(), HttpStatus.CONFLICT.value(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
      String allowed = Arrays.stream(ex.getRequiredType().getEnumConstants())
          .map(Object::toString)
          .collect(Collectors.joining(", "));
      String message = "Invalid value for '" + ex.getName() + "'. Allowed values: " + allowed;
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), message));
    }

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Invalid request parameter"));
  }
}
