package lv.janis.notification_platform.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BootCheckController {

  @GetMapping("/boot-check")
  public Map<String, Object> bootCheck() {
    return Map.of("status", "ok");
  }
}