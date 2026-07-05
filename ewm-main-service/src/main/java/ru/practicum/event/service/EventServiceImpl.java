package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.State;
import ru.practicum.event.model.StateAction;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.EventSpecification;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Дата и время, на которые намечено событие, не могут быть раньше чем через два часа от текущего момента");
        }
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));

        Event event = EventMapper.toEvent(dto, category, initiator);
        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return eventRepository.findByInitiatorId(userId, pageable).stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " у пользователя id=" + userId + " не найдено"));
        return EventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " у пользователя id=" + userId + " не найдено"));

        if (event.getState() == State.PUBLISHED) {
            throw new ConflictException("Изменить можно только отмененные события или события в состоянии ожидания модерации");
        }

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new IllegalArgumentException("Дата события должна быть минимум на 2 часа позже текущего момента");
            }
            event.setEventDate(request.getEventDate());
        }

        updateBaseEventFields(event, request);

        if (request.getStateAction() != null) {
            if (request.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(State.PENDING);
            } else if (request.getStateAction() == StateAction.CANCEL_REVIEW) {
                event.setState(State.CANCELED);
            }
        }

        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<State> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Specification<Event> spec = Specification.where(EventSpecification.hasUsers(users))
                .and(EventSpecification.hasStates(states))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.isBetweenDates(rangeStart, rangeEnd));

        return eventRepository.findAll(spec, pageable).getContent().stream()
                .map(EventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new IllegalArgumentException("Дата начала события должна быть не ранее чем за час от даты публикации");
            }
            event.setEventDate(request.getEventDate());
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != State.PENDING) {
                    throw new ConflictException("Событие можно публиковать только если оно в состоянии ожидания модерации");
                }
                event.setState(State.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction() == StateAction.REJECT_EVENT) {
                if (event.getState() == State.PUBLISHED) {
                    throw new ConflictException("Событие нельзя отклонить, так как оно уже опубликовано");
                }
                event.setState(State.CANCELED);
            }
        }

        updateBaseEventFields(event, request);

        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, int from, int size,
                                               HttpServletRequest request) {
        sendHit(request);

        Sort sorting = Sort.unsorted();
        if ("EVENT_DATE".equals(sort)) {
            sorting = Sort.by(Sort.Direction.ASC, "eventDate");
        } else if ("VIEWS".equals(sort)) {
            sorting = Sort.by(Sort.Direction.DESC, "views");
        }

        Pageable pageable = PageRequest.of(from / size, size, sorting);

        Specification<Event> spec = Specification.where(EventSpecification.hasStates(List.of(State.PUBLISHED)))
                .and(EventSpecification.hasText(text))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.hasPaid(paid))
                .and(EventSpecification.isBetweenDates(rangeStart == null ? LocalDateTime.now() : rangeStart, rangeEnd))
                .and(EventSpecification.hasOnlyAvailable(onlyAvailable));

        return eventRepository.findAll(spec, pageable).getContent().stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto getPublicEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " еще не опубликовано");
        }

        sendHit(request);
        event.setViews(event.getViews() + 1);
        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    private void updateBaseEventFields(Event event, UpdateEventRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + request.getCategory() + " не найдена"));
            event.setCategory(category);
        }
        if (request.getLocation() != null) {
            event.getLocation().setLat(request.getLocation().getLat());
            event.getLocation().setLon(request.getLocation().getLon());
        }
    }

    private void sendHit(HttpServletRequest request) {
        statsClient.saveHit(EndpointHitDto.builder().app("ewm-main-service").uri(request.getRequestURI())
                .ip(request.getRemoteAddr()).timestamp(LocalDateTime.parse(LocalDateTime.now()
                        .format(FORMATTER))).build());
    }
}