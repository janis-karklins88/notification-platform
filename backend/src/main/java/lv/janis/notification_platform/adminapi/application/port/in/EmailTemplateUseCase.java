package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.List;

import lv.janis.notification_platform.adminapi.application.service.EmailTemplateDefinition;

public interface EmailTemplateUseCase {
  List<EmailTemplateDefinition> listTemplates();
}
