package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.delivery.application.port.out.EmailTemplateRepositoryPort;
import lv.janis.notification_platform.delivery.domain.EmailTemplate;

@Repository
public class EmailTemplateRepositoryAdapter implements EmailTemplateRepositoryPort {
  private final EmailTemplateJpaRepository emailTemplateJpaRepository;

  public EmailTemplateRepositoryAdapter(EmailTemplateJpaRepository emailTemplateJpaRepository) {
    this.emailTemplateJpaRepository = emailTemplateJpaRepository;
  }

  @Override
  public EmailTemplate save(EmailTemplate template) {
    return emailTemplateJpaRepository.save(template);
  }

  @Override
  public List<EmailTemplate> findAll() {
    return emailTemplateJpaRepository.findAll();
  }

  @Override
  public Optional<EmailTemplate> findById(UUID id) {
    return emailTemplateJpaRepository.findById(id);
  }

  @Override
  public List<EmailTemplate> findAllByName(String name) {
    return emailTemplateJpaRepository.findAllByName(name);
  }

  @Override
  public Optional<EmailTemplate> findByTenantIdAndName(UUID tenantId, String name) {
    return emailTemplateJpaRepository.findByTenant_IdAndName(tenantId, name);
  }

  @Override
  public List<EmailTemplate> findByTenantId(UUID tenantId) {
    return emailTemplateJpaRepository.findByTenant_Id(tenantId);
  }

}
