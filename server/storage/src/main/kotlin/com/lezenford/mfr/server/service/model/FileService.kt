package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.server.model.entity.File
import com.lezenford.mfr.server.model.repository.FileRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import javax.transaction.Transactional

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

    @Transactional
    @EventListener(ApplicationReadyEvent::class)
    fun normalize() {
        log.info("Normalize files started for build ")
        val files = fileRepository.findAll()
        files.groupBy { it.path }.forEach { (_, values) ->
            if (values.size > 1) {
                if (values.any { it.active }) {
                    fileRepository.deleteAll(values.filter { it.active.not() })
                }
            }
        }
        log.info("Normalize files successfully finished")
        cacheService.cleanGameCaches()
    }

    companion object {
        private val log by Logger()
    }
}