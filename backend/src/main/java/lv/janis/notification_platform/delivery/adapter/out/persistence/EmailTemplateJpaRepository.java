package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import lv.janis.notification_platform.delivery.domain.EmailTemplate;

public interface EmailTemplateJpaRepository extends JpaRepository<EmailTemplate, UUID> {

  List<EmailTemplate> findByTenant_Id(UUID tenantId);

  List<EmailTemplate> findAllByName(String name);

  Optional<EmailTemplate> findByTenant_IdAndName(UUID tenantId, String name);

}
