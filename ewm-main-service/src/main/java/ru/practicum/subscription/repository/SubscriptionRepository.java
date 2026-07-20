package ru.practicum.subscription.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.subscription.model.Subscription;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsBySubscriberIdAndPromoterId(Long subscriberId, Long promoterId);

    Optional<Subscription> findBySubscriberIdAndPromoterId(Long subscriberId, Long promoterId);

    List<Subscription> findAllBySubscriberId(Long subscriberId, Pageable pageable);

    List<Subscription> findAllByPromoterId(Long promoterId, Pageable pageable);
}
