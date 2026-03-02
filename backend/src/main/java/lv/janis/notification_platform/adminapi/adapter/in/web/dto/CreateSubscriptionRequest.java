package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.util.UUID;

public record CreateSubscriptionRequest(String eventType, UUID endpointId) {

}
