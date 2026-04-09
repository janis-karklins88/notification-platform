package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.List;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.EmailTemplate;

public interface EmailTemplateUseCase {
  EmailTemplate createEmailTemplate(CreateEmailTemplateCommand command);

  EmailTemplate getEmailTemplateById(UUID templateId);

  List<EmailTemplate> listAllTemplates();

  List<EmailTemplate> findTemplatesByName(String name);

  EmailTemplate getTemplateByTenantIdAndName(UUID tenantId, String name);

  List<EmailTemplate> listTemplatesByTenantId(UUID tenantId);

  EmailTemplate updateEmailTemplate(UpdateEmailTemplateCommand command);

  void deleteEmailTemplate(UUID templateId);
}
