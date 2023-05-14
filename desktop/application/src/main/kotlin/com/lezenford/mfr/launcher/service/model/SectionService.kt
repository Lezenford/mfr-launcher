package com.lezenford.mfr.launcher.service.model

import com.lezenford.mfr.launcher.config.EXTRA_CACHE
import com.lezenford.mfr.launcher.config.SECTION_CACHE
import com.lezenford.mfr.launcher.model.entity.Section
import com.lezenford.mfr.launcher.model.repository.SectionRepository
import org.hibernate.Hibernate
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SectionService(
    private val sectionRepository: SectionRepository
) {

    @Transactional
    @Cacheable(
        SECTION_CACHE,
        unless = "#result.isEmpty()"
    )
    fun findAll(): List<Section> =
        sectionRepository.findAll().onEach { Hibernate.initialize(it.options) }

    @Cacheable(
        SECTION_CACHE,
        unless = "#result.isEmpty()"
    )
    @Transactional
    fun findAllWithDetails(): List<Section> =
        sectionRepository.findAll().onEach { group ->
            Hibernate.initialize(group.options)
            group.options.forEach { release ->
                Hibernate.initialize(release.files)
            }
        }

    @Transactional
    @CacheEvict(value = [EXTRA_CACHE, SECTION_CACHE], allEntries = true)
    fun save(section: Section) {
        sectionRepository.save(section)
    }

    @Transactional
    @CacheEvict(value = [EXTRA_CACHE, SECTION_CACHE], allEntries = true)
    fun saveAll(sections: List<Section>) {
        sectionRepository.saveAll(sections)
    }

    @Transactional
    @CacheEvict(value = [EXTRA_CACHE, SECTION_CACHE], allEntries = true)
    fun removeAll() {
        sectionRepository.deleteAll()
    }

    @Transactional
    @CacheEvict(value = [EXTRA_CACHE, SECTION_CACHE], allEntries = true)
    fun removeAll(sections: List<Section>) {
        sectionRepository.deleteAll(sections)
    }
}