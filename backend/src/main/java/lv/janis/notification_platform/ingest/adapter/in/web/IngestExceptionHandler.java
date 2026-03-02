package lv.janis.notification_platform.ingest.adapter.in.web;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lv.janis.notification_platform.adminapi.adapter.in.web.dto.ErrorResponse;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;

@RestControllerAdvice(basePackages = "lv.janis.notification_platform.ingest.adapter.in.web")
public class IngestExceptionHandler {

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
}
