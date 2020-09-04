package ru.fullrest.mfr.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.server.configuration.CacheConfiguration;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.repository.UpdateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateService {

    private final UpdateRepository updateRepository;

    @Cacheable(value = CacheConfiguration.UPDATE_CACHE,
            key = "#root.methodName",
            unless = "#result == null || #result.isEmpty()")
    public List<String> findAllActive() {
        return updateRepository.findAllActive();
    }

    @Cacheable(value = CacheConfiguration.UPDATE_CACHE,
            key = "T(ru.fullrest.mfr.server.configuration.CacheConfiguration).UPDATE_CACHE + '|' + #version",
            unless = "#result == null")
    public Update findByVersion(String version) {
        return updateRepository.findByVersion(version);
    }
}
