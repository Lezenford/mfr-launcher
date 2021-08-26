package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.configuration.CacheConfiguration
import com.lezenford.mfr.server.model.entity.TelegramUser
import com.lezenford.mfr.server.model.repository.TelegramUserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TelegramUserService(
    private val telegramUserRepository: TelegramUserRepository
) {
    @Cacheable(value = [CacheConfiguration.USER_CACHE], unless = "#result == null")
    fun findById(userId: Long): TelegramUser? {
        return telegramUserRepository.findById(userId).orElseGet { null }
    }

    @Cacheable(value = [CacheConfiguration.USER_CACHE], unless = "#result.isEmpty()")
    fun findAll(): List<TelegramUser> {
        return telegramUserRepository.findAll().toList()
    }

    @Transactional
    @CacheEvict(value = [CacheConfiguration.USER_CACHE], allEntries = true)
    fun save(user: TelegramUser) {
        telegramUserRepository.save(user)
    }

    @Transactional
    @CacheEvict(value = [CacheConfiguration.USER_CACHE], allEntries = true)
    fun deleteById(id: Long) {
        telegramUserRepository.deleteById(id)
    }
}