package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.server.configuration.CacheConfiguration
import com.lezenford.mfr.server.model.entity.Launcher
import com.lezenford.mfr.server.model.repository.LauncherRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class LauncherService(
    private val launcherRepository: LauncherRepository,
) {

    @Cacheable(value = [CacheConfiguration.LAUNCHER_CACHE], unless = "#result.isEmpty()")
    fun findAll(): List<Launcher> = launcherRepository.findAll()

    @Cacheable(value = [CacheConfiguration.LAUNCHER_CACHE], unless = "#result == null")
    fun findBySystem(system: SystemType): Launcher? = launcherRepository.findBySystem(system)

    @CacheEvict(CacheConfiguration.LAUNCHER_CACHE, allEntries = true)
    fun save(launcher: Launcher) {
        launcherRepository.save(launcher)
    }
}