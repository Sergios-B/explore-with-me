package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto dto) {
        statsRepository.save(StatsMapper.toEndpointHit(dto));
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Время начала не может быть позже времени окончания");
        }

        if (uris == null || uris.isEmpty()) {
            if (unique) {
                return statsRepository.getStatsUniqueIp(start, end);
            } else {
                return statsRepository.getStatsAllIp(start, end);
            }
        } else {
            if (unique) {
                return statsRepository.getStatsUniqueIpWithUris(start, end, uris);
            } else {
                return statsRepository.getStatsAllIpWithUris(start, end, uris);
            }
        }
    }
}