package ru.practicum.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.subscription.dto.SubscriptionDto;
import ru.practicum.subscription.mapper.SubscriptionMapper;
import ru.practicum.subscription.model.Subscription;
import ru.practicum.subscription.repository.SubscriptionRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SubscriptionDto addSubscription(Long userId, Long promoterId) {
        log.info("Добавление подписки: пользователь {} на организатора {}", userId, promoterId);

        if (userId.equals(promoterId)) {
            throw new ConflictException("Вы не можете подписаться на самого себя.");
        }

        User subscriber = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        User promoter = userRepository.findById(promoterId)
                .orElseThrow(() -> new NotFoundException("Организатор с id=" + promoterId + " не найден"));

        if (subscriptionRepository.existsBySubscriberIdAndPromoterId(userId, promoterId)) {
            throw new ConflictException("Вы уже подписаны на этого организатора.");
        }

        Subscription subscription = Subscription.builder()
                .subscriber(subscriber)
                .promoter(promoter)
                .createdAt(LocalDateTime.now())
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionMapper.toSubscriptionDto(saved);
    }

    @Override
    @Transactional
    public void removeSubscription(Long userId, Long promoterId) {
        log.info("Удаление подписки: пользователь {} от организатора {}", userId, promoterId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (!userRepository.existsById(promoterId)) {
            throw new NotFoundException("Организатор с id=" + promoterId + " не найден");
        }

        Subscription subscription = subscriptionRepository.findBySubscriberIdAndPromoterId(userId, promoterId)
                .orElseThrow(() -> new NotFoundException("Подписка не найдена."));

        subscriptionRepository.delete(subscription);
    }

    @Override
    public List<SubscriptionDto> getSubscriptions(Long userId, int from, int size) {
        log.info("Получение списка подписок для пользователя {} (from={}, size={})", userId, from, size);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        return subscriptionRepository.findAllBySubscriberId(userId, pageable).stream()
                .map(SubscriptionMapper::toSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDto> getFollowers(Long promoterId, int from, int size) {
        log.info("Получение списка подписчиков для организатора {} (from={}, size={})", promoterId, from, size);

        if (!userRepository.existsById(promoterId)) {
            throw new NotFoundException("Организатор с id=" + promoterId + " не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        return subscriptionRepository.findAllByPromoterId(promoterId, pageable).stream()
                .map(SubscriptionMapper::toSubscriptionDto)
                .collect(Collectors.toList());
    }
}