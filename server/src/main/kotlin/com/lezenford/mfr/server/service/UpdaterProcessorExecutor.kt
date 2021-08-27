package com.lezenford.mfr.server.service

import com.lezenford.mfr.server.event.SendMessageEvent
import com.lezenford.mfr.server.model.entity.Build
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.fullrest.mfr.common.extensions.Logger
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.PreDestroy
import kotlin.concurrent.withLock

@Service
class UpdaterProcessorExecutor(
    private val gitService: GitService,
    private val storageService: StorageService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val serverGlobalFileLock: ReentrantReadWriteLock
) {
    private val executor = Executors.newSingleThreadExecutor()

    private val running = AtomicBoolean(false)
    private val operation = AtomicReference("")

    fun updateBuild(build: Build) = run("Update build ${build.name}") {
        serverGlobalFileLock.writeLock().withLock {
            log.info("Server start maintenance mod for update task")
            if (gitService.repositoryExist(build).not()) {
                gitService.cloneRepository(build)
            }
            val backupBranch = gitService.updateRepository(build)
            kotlin.runCatching { storageService.updateBuild(build) }
                .onFailure {
                    log.error("Update operation failed")
                    gitService.resetRepositoryTo(build, backupBranch)
                    throw it
                }
            log.info("Server finished maintenance mod for update task")
        }
    }

    private fun run(operation: String, function: () -> Unit): Boolean {
        log.info("Try to start operation $operation")
        if (running.compareAndSet(false, true)) {
            this.operation.set(operation)
            log.info("Operation $operation successfully run")
            val user = SecurityContextHolder.getContext().authentication
            executor.execute {
                var error: Exception? = null
                try {
                    function()
                    log.info("Operation successfully finish")
                } catch (e: Exception) {
                    error = e
                    log.error("Operation has error", e)
                } finally {
                    running.set(false)
                    applicationEventPublisher.publishEvent(
                        SendMessageEvent(
                            source = this,
                            message = SendMessage().apply {
                                chatId = user.principal.toString()
                                text = error?.let { "В процессе выполнения сборки произошла ошибка: ${it.message}" }
                                    ?: "Обновление сборки завершено успешно"
                            }
                        ))
                }
            }
            return true
        } else {
            log.info("Operation" + this.operation.get() + " already running")
            return false
        }
    }

    @PreDestroy
    fun close() {
        executor.shutdown()
    }

    companion object {
        private val log by Logger()
    }
}