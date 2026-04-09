package lv.janis.notification_platform.adminapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEmailTemplateCommand;
import lv.janis.notification_platform.adminapi.application.port.in.EmailTemplateUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEmailTemplateCommand;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.delivery.domain.EmailTemplate;

@WebMvcTest(EmailTemplateAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmailTemplateAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private EmailTemplateUseCase emailTemplateUseCase;

  @MockitoBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void createEmailTemplateReturnsCreated() throws Exception {
    UUID tenantId = UUID.randomUUID();
    EmailTemplate template = emailTemplate(UUID.randomUUID(), tenantId, "welcome", "Welcome", "<p>Hello</p>", true,
        "description", true, Instant.parse("2026-01-01T00:00:00Z"));
    when(emailTemplateUseCase.createEmailTemplate(new CreateEmailTemplateCommand(
        tenantId,
        "welcome",
        "Welcome",
        "<p>Hello</p>",
        true,
        "description"))).thenReturn(template);

    mockMvc.perform(post("/admin/tenants/" + tenantId + "/email-templates")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(new CreateEmailTemplateRequestPayload(
            "welcome",
            "Welcome",
            "<p>Hello</p>",
            true,
            "description"))))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/admin/email-templates/" + template.getId()))
        .andExpect(jsonPath("$.id").value(template.getId().toString()))
        .andExpect(jsonPath("$.name").value("welcome"));
  }

  @Test
  void getEmailTemplateReturnsMapping() throws Exception {
    UUID templateId = UUID.randomUUID();
    EmailTemplate template = emailTemplate(templateId, UUID.randomUUID(), "welcome", "Welcome", "Hello", false,
        "description", true, Instant.parse("2026-01-01T00:00:00Z"));
    when(emailTemplateUseCase.getEmailTemplateById(templateId)).thenReturn(template);

    mockMvc.perform(get("/admin/email-templates/" + templateId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(templateId.toString()))
        .andExpect(jsonPath("$.html").value(false));
  }

  @Test
  void listAllEmailTemplatesReturnsList() throws Exception {
    EmailTemplate template = emailTemplate(UUID.randomUUID(), UUID.randomUUID(), "welcome", "Welcome", "Hello", true,
        "description", true, Instant.parse("2026-01-01T00:00:00Z"));
    when(emailTemplateUseCase.listAllTemplates()).thenReturn(List.of(template));

    mockMvc.perform(get("/admin/email-templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("welcome"));
  }

  @Test
  void findTemplatesByNameReturnsList() throws Exception {
    EmailTemplate template = emailTemplate(UUID.randomUUID(), UUID.randomUUID(), "welcome", "Welcome", "Hello", true,
        "description", true, Instant.parse("2026-01-01T00:00:00Z"));
    when(emailTemplateUseCase.findTemplatesByName("welcome")).thenReturn(List.of(template));

    mockMvc.perform(get("/admin/email-templates/by-name").param("name", "welcome"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("welcome"));
  }

  @Test
  void getTemplateByTenantAndNameReturnsMapping() throws Exception {
    UUID tenantId = UUID.randomUUID();
    EmailTemplate template = emailTemplate(UUID.randomUUID(), tenantId, "welcome", "Welcome", "Hello", true,
        "description", true, Instant.parse("2026-01-01T00:00:00Z"));
    when(emailTemplateUseCase.getTemplateByTenantIdAndName(tenantId, "welcome")).thenReturn(template);

    mockMvc.perform(get("/admin/tenants/" + tenantId + "/email-templates/by-name").param("name", "welcome"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()));
  }

  @Test
  void listEmailTemplatesReturnsList() throws Exception {
    UUID tenantId = UUID.randomUUID();
    EmailTemplate template = emailTemplate(UUID.randomUUID(), tenantId, "welcome", "Welcome", "Hello", true,
        "description", true, Instant.parse("2026-01-01T00:00:00Z"));
    when(emailTemplateUseCase.listTemplatesByTenantId(tenantId)).thenReturn(List.of(template));

    mockMvc.perform(get("/admin/tenants/" + tenantId + "/email-templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tenantId").value(tenantId.toString()));
  }

  @Test
  void updateEmailTemplateReturnsMapping() throws Exception {
    UUID templateId = UUID.randomUUID();
    EmailTemplate template = emailTemplate(templateId, UUID.randomUUID(), "welcome", "Updated", "Body", true,
        "description", true, Instant.parse("2026-01-01T00:00:00Z"));
    when(emailTemplateUseCase.updateEmailTemplate(new UpdateEmailTemplateCommand(
        templateId,
        "welcome",
        "Updated",
        "Body",
        true,
        "description"))).thenReturn(template);

    mockMvc.perform(patch("/admin/email-templates/" + templateId)
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(new UpdateEmailTemplateRequestPayload(
            "welcome",
            "Updated",
            "Body",
            true,
            "description"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("Updated"));
  }

  @Test
  void deleteEmailTemplateReturnsNoContent() throws Exception {
    UUID templateId = UUID.randomUUID();

    mockMvc.perform(post("/admin/email-templates/" + templateId + "/delete"))
        .andExpect(status().isNoContent());

    verify(emailTemplateUseCase).deleteEmailTemplate(templateId);
  }

  @Test
  void getEmailTemplateReturnsNotFound() throws Exception {
    UUID templateId = UUID.randomUUID();
    when(emailTemplateUseCase.getEmailTemplateById(templateId))
        .thenThrow(new NotFoundException("Email template with " + templateId + " not found"));

    mockMvc.perform(get("/admin/email-templates/" + templateId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(templateId.toString())));
  }

  private EmailTemplate emailTemplate(
      UUID id,
      UUID tenantId,
      String name,
      String subject,
      String body,
      boolean html,
      String description,
      boolean active,
      Instant createdAt) {
    EmailTemplate template = Mockito.mock(EmailTemplate.class);
    when(template.getId()).thenReturn(id);
    when(template.getTenantId()).thenReturn(tenantId);
    when(template.getName()).thenReturn(name);
    when(template.getSubject()).thenReturn(subject);
    when(template.getBody()).thenReturn(body);
    when(template.isHtml()).thenReturn(html);
    when(template.getDescription()).thenReturn(description);
    when(template.isActive()).thenReturn(active);
    when(template.getCreatedAt()).thenReturn(createdAt);
    return template;
  }

  private record CreateEmailTemplateRequestPayload(
      String name,
      String subject,
      String body,
      boolean html,
      String description) {
  }

  private record UpdateEmailTemplateRequestPayload(
      String name,
      String subject,
      String body,
      boolean html,
      String description) {
  }
}
