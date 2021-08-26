package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.configuration.CacheConfiguration
import com.lezenford.mfr.server.model.entity.File
import com.lezenford.mfr.server.model.repository.FileRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class FileService(
    private val fileRepository: FileRepository
) {

    @Cacheable(value = [CacheConfiguration.FILE_CACHE], unless = "#result == null")
    fun findById(id: Int): File? = fileRepository.findByIdOrNull(id)
}