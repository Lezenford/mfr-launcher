package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.model.entity.File
import com.lezenford.mfr.server.model.repository.FileRepository
import org.springframework.stereotype.Service

@Service
class FileService(
    private val fileRepository: FileRepository,
    private val cacheService: CacheService
) {
    fun findAllByIds(ids: Collection<Int>): List<File> {
        val result = mutableListOf<File>()
        val nonCacheable = mutableListOf<Int>()
        ids.forEach { id ->
            cacheService.findFile(id)?.also {
                result.add(it)
            } ?: nonCacheable.add(id)
        }
        if (nonCacheable.isNotEmpty()) {
            fileRepository.findAllById(nonCacheable).forEach {
                result.add(cacheService.addFile(it))
            }
        }
        return result
    }
}