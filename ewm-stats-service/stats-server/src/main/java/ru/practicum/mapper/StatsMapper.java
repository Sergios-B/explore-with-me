package ru.practicum.mapper;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.model.EndpointHit;

public class StatsMapper {

    public static EndpointHit toEndpointHit(EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }
}