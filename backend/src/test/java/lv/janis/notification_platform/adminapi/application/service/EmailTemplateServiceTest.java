package lv.janis.notification_platform.adminapi.application.service;

import static lv.janis.notification_platform.support.EntityTestData.emailTemplate;
import static lv.janis.notification_platform.support.EntityTestData.tenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import lv.janis.notification_platform.adminapi.application.exception.ConflictException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEmailTemplateCommand;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEmailTemplateCommand;
import lv.janis.notification_platform.delivery.application.port.out.EmailTemplateRepositoryPort;
import lv.janis.notification_platform.delivery.domain.EmailTemplate;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;

class EmailTemplateServiceTest {
  private final EmailTemplateRepositoryPort emailTemplateRepository = mock(EmailTemplateRepositoryPort.class);
  private final TenantRepositoryPort tenantRepository = mock(TenantRepositoryPort.class);
  private final EmailTemplateService service = new EmailTemplateService(emailTemplateRepository, tenantRepository);

  @Test
  void createEmailTemplatePersistsTemplateWhenTenantExistsAndNameIsUnique() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(emailTemplateRepository.findByTenantIdAndName(tenantId, "welcome")).thenReturn(Optional.empty());
    when(emailTemplateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    EmailTemplate result = service.createEmailTemplate(new CreateEmailTemplateCommand(
        tenantId,
        "welcome",
        "Welcome",
        "<p>Hello</p>",
        true,
        "welcome template"));

    assertSame(tenant, result.getTenant());
    assertEquals("welcome", result.getName());
    assertEquals("Welcome", result.getSubject());
    assertEquals("<p>Hello</p>", result.getBody());
  }

  @Test
  void createEmailTemplateRejectsDuplicateNameWithinTenant() {
    UUID tenantId = UUID.randomUUID();
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId)));
    when(emailTemplateRepository.findByTenantIdAndName(tenantId, "welcome"))
        .thenReturn(Optional.of(emailTemplate(UUID.randomUUID(), tenant(tenantId), "welcome", "s", "b", true, null, true)));

    ConflictException ex = assertThrows(
        ConflictException.class,
        () -> service.createEmailTemplate(new CreateEmailTemplateCommand(
            tenantId,
            "welcome",
            "Welcome",
            "Hello",
            false,
            null)));

    assertEquals("Email template name already exists for tenant: welcome", ex.getMessage());
  }

  @Test
  void getEmailTemplateByIdReturnsTemplate() {
    UUID templateId = UUID.randomUUID();
    EmailTemplate template = mock(EmailTemplate.class);
    when(emailTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

    EmailTemplate result = service.getEmailTemplateById(templateId);

    assertSame(template, result);
  }

  @Test
  void findTemplatesByNameReturnsOnlyActiveTemplates() {
    UUID tenantId = UUID.randomUUID();
    EmailTemplate active = emailTemplate(UUID.randomUUID(), tenant(tenantId), "welcome", "subject", "body", true, null, true);
    EmailTemplate inactive = emailTemplate(UUID.randomUUID(), tenant(tenantId), "welcome", "subject", "body", true, null, false);
    when(emailTemplateRepository.findAllByName("welcome")).thenReturn(List.of(active, inactive));

    List<EmailTemplate> result = service.findTemplatesByName("welcome");

    assertEquals(List.of(active), result);
  }

  @Test
  void getTemplateByTenantIdAndNameRejectsInactiveTemplate() {
    UUID tenantId = UUID.randomUUID();
    when(emailTemplateRepository.findByTenantIdAndName(tenantId, "welcome"))
        .thenReturn(Optional.of(emailTemplate(UUID.randomUUID(), tenant(tenantId), "welcome", "subject", "body", true, null, false)));

    NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> service.getTemplateByTenantIdAndName(tenantId, "welcome"));

    assertEquals("Active email template with name welcome not found for tenant " + tenantId, ex.getMessage());
  }

  @Test
  void listTemplatesByTenantIdReturnsOnlyActiveTemplates() {
    UUID tenantId = UUID.randomUUID();
    EmailTemplate active = emailTemplate(UUID.randomUUID(), tenant(tenantId), "one", "subject", "body", false, null, true);
    EmailTemplate inactive = emailTemplate(UUID.randomUUID(), tenant(tenantId), "two", "subject", "body", false, null, false);
    when(emailTemplateRepository.findByTenantId(tenantId)).thenReturn(List.of(active, inactive));

    List<EmailTemplate> result = service.listTemplatesByTenantId(tenantId);

    assertEquals(List.of(active), result);
  }

  @Test
  void updateEmailTemplateEditsAndSavesTemplate() {
    UUID tenantId = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    EmailTemplate template = emailTemplate(templateId, tenant(tenantId), "old", "old subject", "old body", false, "old", true);
    when(emailTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
    when(emailTemplateRepository.findByTenantIdAndName(tenantId, "new")).thenReturn(Optional.empty());
    when(emailTemplateRepository.save(template)).thenReturn(template);

    EmailTemplate result = service.updateEmailTemplate(new UpdateEmailTemplateCommand(
        templateId,
        "new",
        "New subject",
        "<p>New body</p>",
        true,
        "updated"));

    assertSame(template, result);
    assertEquals("new", template.getName());
    assertEquals("New subject", template.getSubject());
    assertEquals("<p>New body</p>", template.getBody());
    assertEquals("updated", template.getDescription());
  }

  @Test
  void deleteEmailTemplateMarksTemplateInactive() {
    UUID templateId = UUID.randomUUID();
    EmailTemplate template = emailTemplate(templateId, tenant(UUID.randomUUID()), "welcome", "subject", "body", true, null, true);
    when(emailTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
    when(emailTemplateRepository.save(template)).thenReturn(template);

    service.deleteEmailTemplate(templateId);

    assertFalse(template.isActive());
    verify(emailTemplateRepository).save(template);
  }
}
