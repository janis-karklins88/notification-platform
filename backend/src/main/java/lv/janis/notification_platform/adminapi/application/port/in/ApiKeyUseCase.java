package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import org.springframework.data.domain.Page;

import lv.janis.notification_platform.auth.application.port.out.ListApiKeyQuery;
import lv.janis.notification_platform.auth.domain.ApiKey;

public interface ApiKeyUseCase {

  CreateApiKeyResult createApiKey(UUID tenantId);

  Page<ApiKey> listApiKeys(ListApiKeyQuery query);

  void revokeApiKey(UUID apiKeyId);
}
