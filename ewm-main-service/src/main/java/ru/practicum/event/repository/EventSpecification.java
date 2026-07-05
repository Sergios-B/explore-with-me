package ru.practicum.event.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.State;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> hasUsers(List<Long> users) {
        return (root, query, cb) -> users == null || users.isEmpty() ?
                cb.conjunction() : root.get("initiator").get("id").in(users);
    }

    public static Specification<Event> hasStates(List<State> states) {
        return (root, query, cb) -> states == null || states.isEmpty() ?
                cb.conjunction() : root.get("state").in(states);
    }

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, cb) -> categories == null || categories.isEmpty() ?
                cb.conjunction() : root.get("category").get("id").in(categories);
    }

    public static Specification<Event> isBetweenDates(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return cb.conjunction();
            if (start != null && end != null) return cb.between(root.get("eventDate"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("eventDate"), start);
            return cb.lessThanOrEqualTo(root.get("eventDate"), end);
        };
    }

    public static Specification<Event> hasText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) return cb.conjunction();
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Event> hasPaid(Boolean paid) {
        return (root, query, cb) -> paid == null ? cb.conjunction() : cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> hasOnlyAvailable(Boolean onlyAvailable) {
        return (root, query, cb) -> {
            if (onlyAvailable == null || !onlyAvailable) return cb.conjunction();
            return cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
            );
        };
    }
}