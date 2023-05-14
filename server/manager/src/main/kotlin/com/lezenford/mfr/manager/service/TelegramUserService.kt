package com.lezenford.mfr.manager.service

import com.lezenford.mfr.manager.configuration.MODULE_CACHE
import com.lezenford.mfr.manager.configuration.USER_CACHE
import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.model.repository.TelegramUserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TelegramUserService(
    private val telegramUserRepository: TelegramUserRepository
) {
    @Cacheable(value = [USER_CACHE], key = "T(java.lang.Long).toString(#userId)", unless = "#result == null")
    suspend fun findById(userId: Long): TelegramUser? {
        userId.toString()
        return telegramUserRepository.findByTelegramId(userId)
    }

    fun findAll(): Flow<TelegramUser> {
        return telegramUserRepository.findAll()
    }

    @Transactional
    @CacheEvict(value = [USER_CACHE, MODULE_CACHE], allEntries = true)
    suspend fun save(user: TelegramUser) {
        telegramUserRepository.save(user)
    }

    @Transactional
    @CacheEvict(value = [USER_CACHE, MODULE_CACHE], allEntries = true)
    suspend fun deleteById(id: Long) {
        telegramUserRepository.deleteById(id)
    }
}