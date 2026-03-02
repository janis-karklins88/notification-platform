package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import org.springframework.data.domain.Page;

import lv.janis.notification_platform.routing.domain.Subscription;

public interface SubscriptionUseCase {
  public Subscription createSubscription(CreateSubscriptionCommand command);

  public Page<Subscription> listSubscriptions(ListSubscriptionsQuery query);

  public void deleteSubscription(UUID subscriptionId);

  public void activateSubscription(UUID subscriptionId);

  public void deactivateSubscription(UUID subscriptionId);
}
