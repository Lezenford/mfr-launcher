package com.lezenford.mfr.server.service

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

@Service
class MaintenanceService {
    private val maintenance = ConcurrentHashMap<Type, LocalDateTime>()

    fun setUp(type: Type): Boolean {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val putIfAbsent = maintenance.putIfAbsent(type, now)
        return putIfAbsent == now
    }

    fun setDown(type: Type): Boolean {
        return maintenance.remove(type) != null
    }

    fun maintenance(type: Type): Boolean = (maintenance[type] ?: maintenance[Type.ALL]) != null

    enum class Type {
        GAME, LAUNCHER, MANUAL, ALL
    }
}