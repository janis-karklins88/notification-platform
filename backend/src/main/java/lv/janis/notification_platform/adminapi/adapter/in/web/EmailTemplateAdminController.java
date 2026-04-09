package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lv.janis.notification_platform.adminapi.adapter.in.web.dto.EmailTemplateResponse;
import lv.janis.notification_platform.adminapi.application.port.in.EmailTemplateUseCase;

@RestController
@Validated
@RequestMapping("/admin")
public class EmailTemplateAdminController {
  private final EmailTemplateUseCase emailTemplateUseCase;

  public EmailTemplateAdminController(EmailTemplateUseCase emailTemplateUseCase) {
    this.emailTemplateUseCase = emailTemplateUseCase;
  }

  @GetMapping("/email-templates")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<List<EmailTemplateResponse>> listEmailTemplates() {
    var response = emailTemplateUseCase.listTemplates().stream()
        .map(EmailTemplateResponse::from)
        .toList();
    return ResponseEntity.ok(response);
  }
}
