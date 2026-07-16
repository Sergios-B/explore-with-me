package ru.practicum.subscription.service;

import ru.practicum.subscription.dto.SubscriptionDto;
import java.util.List;

public interface SubscriptionService {

    SubscriptionDto addSubscription(Long userId, Long promoterId);

    void removeSubscription(Long userId, Long promoterId);

    List<SubscriptionDto> getSubscriptions(Long userId, int from, int size);

    List<SubscriptionDto> getFollowers(Long promoterId, int from, int size);
}
