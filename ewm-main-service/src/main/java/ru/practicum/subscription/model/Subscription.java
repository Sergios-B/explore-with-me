package ru.practicum.subscription.model;

import lombok.*;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private User subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoter_id", nullable = false)
    private User promoter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
