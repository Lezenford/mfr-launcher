package com.lezenford.mfr.manager.model.repository

import com.lezenford.mfr.manager.model.entity.TelegramUser
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TelegramUserRepository : CoroutineCrudRepository<TelegramUser, Long> {

    suspend fun findByTelegramId(telegramId: Long): TelegramUser?
}