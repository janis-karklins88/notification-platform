package lv.janis.notification_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import lv.janis.notification_platform.outbox.application.service.OutboxDispatchProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OutboxDispatchProperties.class)
public class NotificationPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationPlatformApplication.class, args);
	}

}
