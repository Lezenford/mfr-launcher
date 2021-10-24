package com.lezenford.mfr.server.service

import com.lezenford.mfr.server.component.ServerStatus
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
import javax.annotation.PreDestroy

@Service
class UpdaterProcessorExecutor(
    private val buildGitService: BuildGitService,
    private val manualGitService: ManualGitService,
    private val storageService: StorageService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val executor = Executors.newSingleThreadExecutor()

    private val running = AtomicBoolean(false)
    private val operation = AtomicReference("")

    fun updateBuild(build: Build): Boolean {
        val user = SecurityContextHolder.getContext().authentication
        return run("Update build ${build.name}") {
            ServerStatus.maintenance {
                applicationEventPublisher.publishEvent(
                    SendMessageEvent(
                        source = this,
                        message = SendMessage().apply {
                            chatId = user.principal.toString()
                            text = "Сервер переведен в режим обновлений. Процесс обновления сборки запущен."
                        }
                    ))
                log.info("Server start maintenance mod for update task")
                if (buildGitService.repositoryExist(build).not()) {
                    buildGitService.cloneRepository(build)
                }
                val backupBranch = buildGitService.updateRepository(build)
                kotlin.runCatching { storageService.updateBuild(build) }
                    .onFailure {
                        log.error("Update operation failed")
                        buildGitService.resetRepositoryTo(build, backupBranch)
                        throw it
                    }
                log.info("Server finished maintenance mod for update task")
            }
        }
    }

    fun updateManual(): Boolean {
        return run("Update manual") {
            log.info("Server start manual update task")
            if (manualGitService.repositoryExist().not()) {
                manualGitService.cloneRepository()
            }
            manualGitService.updateRepository()
            log.info("Server successfully finished manual update task")
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
                    log.info("Update executor started")
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
                                text = error?.let { "В процессе обновления произошла ошибка: ${it.message}" }
                                    ?: "Обновление завершено успешно"
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