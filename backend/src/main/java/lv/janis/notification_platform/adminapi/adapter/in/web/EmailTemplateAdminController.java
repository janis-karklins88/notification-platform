package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.CreateEmailTemplateRequest;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.EmailTemplateResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.UpdateEmailTemplateRequest;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEmailTemplateCommand;
import lv.janis.notification_platform.adminapi.application.port.in.EmailTemplateUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEmailTemplateCommand;

@RestController
@Validated
@RequestMapping("/admin")
public class EmailTemplateAdminController {
  private final EmailTemplateUseCase emailTemplateUseCase;

  public EmailTemplateAdminController(EmailTemplateUseCase emailTemplateUseCase) {
    this.emailTemplateUseCase = emailTemplateUseCase;
  }

  @PostMapping("/tenants/{tenantId}/email-templates")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<EmailTemplateResponse> createEmailTemplate(
      @PathVariable UUID tenantId,
      @Valid @RequestBody CreateEmailTemplateRequest request) {
    var command = new CreateEmailTemplateCommand(
        tenantId,
        request.name(),
        request.subject(),
        request.body(),
        request.html(),
        request.description());
    var result = emailTemplateUseCase.createEmailTemplate(command);
    return ResponseEntity.created(URI.create("/admin/email-templates/" + result.getId()))
        .body(EmailTemplateResponse.from(result));
  }

  @GetMapping("/email-templates/{templateId}")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<EmailTemplateResponse> getEmailTemplateById(@PathVariable UUID templateId) {
    return ResponseEntity.ok(EmailTemplateResponse.from(emailTemplateUseCase.getEmailTemplateById(templateId)));
  }

  @GetMapping("/email-templates/by-name")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<List<EmailTemplateResponse>> findTemplatesByName(@RequestParam @NotBlank String name) {
    var response = emailTemplateUseCase.findTemplatesByName(name).stream()
        .map(EmailTemplateResponse::from)
        .toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/tenants/{tenantId}/email-templates/by-name")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<EmailTemplateResponse> getTemplateByTenantAndName(
      @PathVariable UUID tenantId,
      @RequestParam @NotBlank String name) {
    var result = emailTemplateUseCase.getTemplateByTenantIdAndName(tenantId, name);
    return ResponseEntity.ok(EmailTemplateResponse.from(result));
  }

  @GetMapping("/tenants/{tenantId}/email-templates")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<List<EmailTemplateResponse>> listEmailTemplates(@PathVariable UUID tenantId) {
    var response = emailTemplateUseCase.listTemplatesByTenantId(tenantId).stream()
        .map(EmailTemplateResponse::from)
        .toList();
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/email-templates/{templateId}")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<EmailTemplateResponse> updateEmailTemplate(
      @PathVariable UUID templateId,
      @Valid @RequestBody UpdateEmailTemplateRequest request) {
    var command = new UpdateEmailTemplateCommand(
        templateId,
        request.name(),
        request.subject(),
        request.body(),
        request.html(),
        request.description());
    var result = emailTemplateUseCase.updateEmailTemplate(command);
    return ResponseEntity.ok(EmailTemplateResponse.from(result));
  }

  @PostMapping("/email-templates/{templateId}/delete")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> deleteEmailTemplate(@PathVariable UUID templateId) {
    emailTemplateUseCase.deleteEmailTemplate(templateId);
    return ResponseEntity.noContent().build();
  }
}
