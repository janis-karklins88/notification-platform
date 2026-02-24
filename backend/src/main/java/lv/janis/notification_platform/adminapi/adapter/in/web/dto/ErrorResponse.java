package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error) {
}
