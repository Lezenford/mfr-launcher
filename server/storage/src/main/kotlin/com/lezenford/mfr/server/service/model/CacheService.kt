package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.configuration.CacheConfiguration.Companion.CATEGORY_CACHE
import com.lezenford.mfr.server.configuration.CacheConfiguration.Companion.FILE_CACHE
import com.lezenford.mfr.server.model.entity.File
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CacheService {

    @Cacheable(FILE_CACHE, key = "#id", unless = "#result == null")
    fun findFile(id: Int): File? = null

    @CachePut(FILE_CACHE, key = "#file.id")
    fun addFile(file: File) = file

    @CacheEvict(value = [CATEGORY_CACHE, FILE_CACHE], allEntries = true)
    fun cleanGameCaches() {
    }
}