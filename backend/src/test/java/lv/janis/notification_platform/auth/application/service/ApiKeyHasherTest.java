package lv.janis.notification_platform.auth.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ApiKeyHasherTest {
  private final ApiKeyHasher hasher = new ApiKeyHasher();

  @Test
  void hashReturnsDeterministicSha256Hex() {
    String hash = hasher.hash("abc");

    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash);
  }
}
