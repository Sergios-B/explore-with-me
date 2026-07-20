package ru.practicum.subscription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.subscription.dto.SubscriptionDto;
import ru.practicum.subscription.service.SubscriptionService;

import jakarta.validation.constraints.*;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SubscriptionPrivateController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{promoterId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto addSubscription(@PathVariable Long userId,
                                           @PathVariable Long promoterId) {
        log.info("HTTP POST: Пользователь {} подписывается на организатора {}", userId, promoterId);
        return subscriptionService.addSubscription(userId, promoterId);
    }

    @DeleteMapping("/{promoterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSubscription(@PathVariable Long userId,
                                   @PathVariable Long promoterId) {
        log.info("HTTP DELETE: Пользователь {} отменяет подписку на организатора {}", userId, promoterId);
        subscriptionService.removeSubscription(userId, promoterId);
    }

    @GetMapping
    public List<SubscriptionDto> getSubscriptions(@PathVariable Long userId,
                                                  @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                  @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("HTTP GET: Получение подписок пользователя {} (from={}, size={})", userId, from, size);
        return subscriptionService.getSubscriptions(userId, from, size);
    }

    @GetMapping("/followers")
    public List<SubscriptionDto> getFollowers(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                              @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("HTTP GET: Получение подписчиков пользователя {} (from={}, size={})", userId, from, size);
        return subscriptionService.getFollowers(userId, from, size);
    }
}
