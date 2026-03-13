package lv.janis.notification_platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;

@WebMvcTest(BootCheckController.class)
@AutoConfigureMockMvc(addFilters = false)
class BootCheckControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void bootCheckReturnsStatusOk() throws Exception {
    mockMvc.perform(get("/boot-check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }
}
