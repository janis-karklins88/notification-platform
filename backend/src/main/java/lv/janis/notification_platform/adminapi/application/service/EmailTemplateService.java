package lv.janis.notification_platform.adminapi.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.exception.ConflictException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEmailTemplateCommand;
import lv.janis.notification_platform.adminapi.application.port.in.EmailTemplateUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEmailTemplateCommand;
import lv.janis.notification_platform.delivery.application.port.out.EmailTemplateRepositoryPort;
import lv.janis.notification_platform.delivery.domain.EmailTemplate;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;

@Service
public class EmailTemplateService implements EmailTemplateUseCase {
  private final EmailTemplateRepositoryPort emailTemplateRepository;
  private final TenantRepositoryPort tenantRepository;

  public EmailTemplateService(
      EmailTemplateRepositoryPort emailTemplateRepository,
      TenantRepositoryPort tenantRepository) {
    this.emailTemplateRepository = emailTemplateRepository;
    this.tenantRepository = tenantRepository;
  }

  @Override
  @Transactional
  public EmailTemplate createEmailTemplate(CreateEmailTemplateCommand command) {
    var tenant = tenantRepository.findById(command.tenantId())
        .orElseThrow(() -> new NotFoundException("Tenant with " + command.tenantId() + " not found"));
    ensureTemplateNameIsAvailable(command.tenantId(), command.name(), null);

    EmailTemplate template = new EmailTemplate(
        tenant,
        command.name(),
        command.subject(),
        command.body(),
        command.html(),
        command.description());
    return emailTemplateRepository.save(template);
  }

  @Override
  @Transactional(readOnly = true)
  public EmailTemplate getEmailTemplateById(UUID templateId) {
    return emailTemplateRepository.findById(templateId)
        .orElseThrow(() -> new NotFoundException("Email template with " + templateId + " not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EmailTemplate> listAllTemplates() {
    return activeOnly(emailTemplateRepository.findAll());
  }

  @Override
  @Transactional(readOnly = true)
  public List<EmailTemplate> findTemplatesByName(String name) {
    return activeOnly(emailTemplateRepository.findAllByName(name));
  }

  @Override
  @Transactional(readOnly = true)
  public EmailTemplate getTemplateByTenantIdAndName(UUID tenantId, String name) {
    return emailTemplateRepository.findByTenantIdAndName(tenantId, name)
        .filter(EmailTemplate::isActive)
        .orElseThrow(() -> new NotFoundException(
            "Active email template with name " + name + " not found for tenant " + tenantId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EmailTemplate> listTemplatesByTenantId(UUID tenantId) {
    return activeOnly(emailTemplateRepository.findByTenantId(tenantId));
  }

  @Override
  @Transactional
  public EmailTemplate updateEmailTemplate(UpdateEmailTemplateCommand command) {
    EmailTemplate template = emailTemplateRepository.findById(command.templateId())
        .orElseThrow(() -> new NotFoundException("Email template with " + command.templateId() + " not found"));

    ensureTemplateNameIsAvailable(template.getTenantId(), command.name(), template.getId());
    template.edit(command.name(), command.subject(), command.body(), command.html(), command.description());
    return emailTemplateRepository.save(template);
  }

  @Override
  @Transactional
  public void deleteEmailTemplate(UUID templateId) {
    EmailTemplate template = emailTemplateRepository.findById(templateId)
        .orElseThrow(() -> new NotFoundException("Email template with " + templateId + " not found"));
    template.delete();
    emailTemplateRepository.save(template);
  }

  private void ensureTemplateNameIsAvailable(UUID tenantId, String name, UUID currentTemplateId) {
    emailTemplateRepository.findByTenantIdAndName(tenantId, name)
        .filter(existing -> currentTemplateId == null || !existing.getId().equals(currentTemplateId))
        .ifPresent(existing -> {
          throw ConflictException.of("Email template name already exists for tenant: " + name);
        });
  }

  private static List<EmailTemplate> activeOnly(List<EmailTemplate> templates) {
    return templates.stream()
        .filter(EmailTemplate::isActive)
        .toList();
  }
}
