package ru.fullrest.mfr.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.server.configuration.CacheConfiguration;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.repository.TelegramUserRepository;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;

    @Cacheable(value = CacheConfiguration.USER_CACHE,
            key = "T(ru.fullrest.mfr.server.configuration.CacheConfiguration).USER_CACHE + '|' + #userId",
            unless = "#result == null")
    public TelegramUser findById(Integer userId) {
        return telegramUserRepository.findById(userId).orElse(null);
    }

    @Cacheable(value = CacheConfiguration.USER_CACHE,
            key = "#root.methodName",
            unless = "#result == null")
    public Iterable<TelegramUser> findAll() {
        return telegramUserRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = CacheConfiguration.USER_CACHE, allEntries = true)
    public void save(TelegramUser user) {
        telegramUserRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = CacheConfiguration.USER_CACHE, allEntries = true)
    public void delete(TelegramUser user) {
        telegramUserRepository.delete(user);
    }
}
