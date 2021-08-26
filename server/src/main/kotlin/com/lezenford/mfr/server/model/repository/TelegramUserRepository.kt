package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.TelegramUser
import org.springframework.data.jpa.repository.JpaRepository

interface TelegramUserRepository : JpaRepository<TelegramUser, Long> {
}