package ru.fullrest.mfr.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.server.configuration.CacheConfiguration;
import ru.fullrest.mfr.server.model.dto.StatisticsCountDto;
import ru.fullrest.mfr.server.model.entity.ApplicationDownloadHistory;
import ru.fullrest.mfr.server.model.entity.GameDownloadHistory;
import ru.fullrest.mfr.server.model.entity.PropertyType;
import ru.fullrest.mfr.server.model.entity.UpdateDownloadHistory;
import ru.fullrest.mfr.server.model.repository.ApplicationDownloadHistoryRepository;
import ru.fullrest.mfr.server.model.repository.GameDownloadHistoryRepository;
import ru.fullrest.mfr.server.model.repository.PropertyRepository;
import ru.fullrest.mfr.server.model.repository.UpdateDownloadHistoryRepository;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class HistoryService {
    private final PropertyRepository propertyRepository;
    private final ApplicationDownloadHistoryRepository applicationDownloadHistoryRepository;
    private final GameDownloadHistoryRepository gameDownloadHistoryRepository;
    private final UpdateDownloadHistoryRepository updateDownloadHistoryRepository;

    @Transactional
    public void applicationDownload(String cookie) {
        if (cookie != null) {
            String key = getKeyFromCookie(cookie);
            if (key != null) {
                String version = propertyRepository.findByType(PropertyType.LAUNCHER_VERSION).orElseThrow().getValue();
                if (!applicationDownloadHistoryRepository.existsByClientKeyAndVersion(key, version)) {
                    ApplicationDownloadHistory applicationDownloadHistory = new ApplicationDownloadHistory();
                    applicationDownloadHistory.setClientKey(key);
                    applicationDownloadHistory.setVersion(version);
                    applicationDownloadHistoryRepository.save(applicationDownloadHistory);
                }
            }
        }
    }

    @Transactional
    public void gameDownload(String cookie) {
        if (cookie != null) {
            String key = getKeyFromCookie(cookie);
            if (key != null) {
                if (!gameDownloadHistoryRepository.existsByClientKey(key)) {
                    GameDownloadHistory gameDownloadHistory = new GameDownloadHistory();
                    gameDownloadHistory.setClientKey(key);
                    gameDownloadHistoryRepository.save(gameDownloadHistory);
                }
            }
        }
    }

    @Transactional
    public void updateDownload(String cookie, String version) {
        if (cookie != null) {
            String key = getKeyFromCookie(cookie);
            if (key != null) {
                if (!updateDownloadHistoryRepository.existsByClientKeyAndVersion(key, version)) {
                    UpdateDownloadHistory updateDownloadHistory = new UpdateDownloadHistory();
                    updateDownloadHistory.setClientKey(key);
                    updateDownloadHistory.setVersion(version);
                    updateDownloadHistoryRepository.save(updateDownloadHistory);
                }
            }
        }
    }

    @Cacheable(value = CacheConfiguration.STATISTICS_CACHE, key = "#root.methodName")
    public int getTotalUserCount() {
        HashSet<String> set = new HashSet<>();
        gameDownloadHistoryRepository.findAll().forEach(it -> set.add(it.getClientKey()));
        applicationDownloadHistoryRepository.findAll().forEach(it -> set.add(it.getClientKey()));
        updateDownloadHistoryRepository.findAll().forEach(it -> set.add(it.getClientKey()));
        return set.size();
    }

    @Cacheable(value = CacheConfiguration.STATISTICS_CACHE, key = "#root.methodName")
    public int getGameDownloadCount() {
        return gameDownloadHistoryRepository.countAll();
    }

    @Cacheable(value = CacheConfiguration.STATISTICS_CACHE, key = "#root.methodName")
    public List<StatisticsCountDto> getApplicationDownloadCount() {
        return applicationDownloadHistoryRepository.countAll();
    }

    @Cacheable(value = CacheConfiguration.STATISTICS_CACHE, key = "#root.methodName")
    public List<StatisticsCountDto> getUpdateDownloadCount() {
        return updateDownloadHistoryRepository.countAll();
    }

    private String getKeyFromCookie(String cookie) {
        Optional<String> first = Arrays.stream(cookie.split(";")).filter(it -> it.contains("Key=")).findFirst();
        return first.map(s -> s.trim().replace("Key=", "")).orElse(null);
    }
}
