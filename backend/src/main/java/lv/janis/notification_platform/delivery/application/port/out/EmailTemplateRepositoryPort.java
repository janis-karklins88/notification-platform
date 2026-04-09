package lv.janis.notification_platform.delivery.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.EmailTemplate;

public interface EmailTemplateRepositoryPort {

  EmailTemplate save(EmailTemplate template);

  Optional<EmailTemplate> findById(UUID id);

  List<EmailTemplate> findAllByName(String name);

  Optional<EmailTemplate> findByTenantIdAndName(UUID tenantId, String name);

  List<EmailTemplate> findByTenantId(UUID tenantId);
}
