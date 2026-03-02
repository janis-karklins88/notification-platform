package lv.janis.notification_platform.ingest.adapter.in.web;

import org.springframework.http.ResponseEntity;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lv.janis.notification_platform.auth.application.security.ApiKeyPrincipal;
import lv.janis.notification_platform.ingest.adapter.in.web.dto.EventResponse;
import lv.janis.notification_platform.ingest.adapter.in.web.dto.IngestRequest;
import lv.janis.notification_platform.ingest.adapter.in.web.dto.IngestResponse;
import lv.janis.notification_platform.ingest.application.port.in.IngestCommand;
import lv.janis.notification_platform.ingest.application.port.in.IngestUseCase;

@RestController
@Validated
@RequestMapping("/ingest")
public class IngestController {
  private final IngestUseCase ingestUseCase;

  public IngestController(IngestUseCase ingestUseCase) {
    this.ingestUseCase = ingestUseCase;
  }

  @PostMapping
  public ResponseEntity<IngestResponse> ingest(
      @AuthenticationPrincipal ApiKeyPrincipal principal,
      @Valid @RequestBody IngestRequest request) {
    var command = new IngestCommand(
        principal.tenantId(),
        request.eventType(),
        request.payload(),
        request.idempotencyKey(),
        request.source(),
        request.traceId());
    var result = ingestUseCase.ingest(command);
    var response = IngestResponse.from(result);

    if (result.duplicate()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/events/{eventId}")
  public ResponseEntity<EventResponse> getEvent(
      @AuthenticationPrincipal ApiKeyPrincipal principal,
      @PathVariable UUID eventId) {
    var event = ingestUseCase.getEventById(principal.tenantId(), eventId);
    var response = EventResponse.from(event);
    return ResponseEntity.ok(response);
  }
}
