package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.configuration.CacheConfiguration
import com.lezenford.mfr.server.model.entity.Property
import com.lezenford.mfr.server.model.repository.PropertyRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository
) {
    @Cacheable(value = [CacheConfiguration.PROPERTY_CACHE], unless = "#result == null")
    suspend fun findByType(type: Property.Type): Property? {
        return propertyRepository.findByType(type)
    }

    @Transactional
    @CacheEvict(value = [CacheConfiguration.PROPERTY_CACHE], allEntries = true)
    suspend fun save(property: Property) {
        propertyRepository.save(property)
    }
}