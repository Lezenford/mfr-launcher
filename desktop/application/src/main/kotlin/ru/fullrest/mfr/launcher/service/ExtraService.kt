package ru.fullrest.mfr.launcher.service

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import ru.fullrest.mfr.launcher.config.EXTRA_CACHE
import ru.fullrest.mfr.launcher.config.SECTION_CACHE
import ru.fullrest.mfr.launcher.model.entity.Extra
import ru.fullrest.mfr.launcher.model.repository.ExtraRepository
import javax.transaction.Transactional

@Service
class ExtraService(
    private val extraRepository: ExtraRepository
) {

    @Cacheable(
        EXTRA_CACHE,
        unless = "#result.isEmpty()"
    )
    fun findAll(): List<Extra> = extraRepository.findAll()

    @Transactional
    @CacheEvict(value = [EXTRA_CACHE, SECTION_CACHE], allEntries = true)
    fun save(extra: Extra) {
        extraRepository.save(extra)
    }

    @Transactional
    @CacheEvict(value = [EXTRA_CACHE, SECTION_CACHE], allEntries = true)
    fun saveAll(extras: List<Extra>) {
        extraRepository.saveAll(extras)
    }

    @Transactional
    @CacheEvict(value = [EXTRA_CACHE, SECTION_CACHE], allEntries = true)
    fun removeAll() {
        extraRepository.deleteAll()
    }
}