package ru.tms.services;

import ru.tms.dto.Subscription;
import ru.tms.entity.SubscriptionEntity;
import ru.tms.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface SubscriptionService {

    SubscriptionEntity getSubscriptionById(Long subscriptionId);

    Optional<SubscriptionEntity> getSubscriptionByUserIdAndName(String name, UserEntity userEntity);

    List<Subscription> getSubscriptionsByUserId(Long userId);

    List<String> findTopThreeSubscriptions();

    Subscription createSubscription(Subscription subscription, Long userId);

    void deleteSubscription(Long userId, Long subscriptionId);
}
