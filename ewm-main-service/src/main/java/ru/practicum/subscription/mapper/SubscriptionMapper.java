package ru.practicum.subscription.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.subscription.model.Subscription;
import ru.practicum.subscription.dto.SubscriptionDto;
import ru.practicum.user.mapper.UserMapper;

@UtilityClass
public class SubscriptionMapper {

    public static SubscriptionDto toSubscriptionDto(Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        return SubscriptionDto.builder()
                .id(subscription.getId())
                .subscriber(UserMapper.toUserShortDto(subscription.getSubscriber()))
                .promoter(UserMapper.toUserShortDto(subscription.getPromoter()))
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
