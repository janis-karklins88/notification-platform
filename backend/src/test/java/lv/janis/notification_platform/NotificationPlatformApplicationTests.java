package lv.janis.notification_platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Full application context smoke test is environment-dependent and covered later with dedicated integration setup")
@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.mail.host=localhost",
		"spring.mail.port=1025"
})
class NotificationPlatformApplicationTests {

	@Test
	void contextLoads() {
	}

}
