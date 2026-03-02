package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

public record CreateSubscriptionCommand(UUID tenantId, String eventType, UUID endpointId) {

}
