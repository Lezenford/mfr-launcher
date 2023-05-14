package com.lezenford.mfr.server.service

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.server.annotation.Maintenance
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.model.entity.Launcher
import com.lezenford.mfr.server.service.git.BuildGitService
import com.lezenford.mfr.server.service.git.ManualGitService
import com.lezenford.mfr.server.service.model.BuildService
import com.lezenford.mfr.server.service.model.CacheService
import com.lezenford.mfr.server.service.model.LauncherService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize

@Service
class UpdaterService(
    private val buildGitService: BuildGitService,
    private val manualGitService: ManualGitService,
    private val storageService: StorageService,
    private val buildService: BuildService,
    private val launcherService: LauncherService,
    private val properties: ServerSettingProperties,
    private val cacheService: CacheService,
    private val streamService: StreamService
) {
    private val currentTasks: MutableMap<Operation, LocalDateTime> = ConcurrentHashMap<Operation, LocalDateTime>()

    @Maintenance(MaintenanceService.Type.GAME)
    suspend fun updateBuild(buildId: Int) = run(Operation.GAME) {
        val build = buildService.findById(buildId)
            ?: throw IllegalArgumentException("Build with id $buildId doesn't exist")
        if (buildGitService.repositoryExist(build).not()) {
            buildGitService.cloneRepository(build)
        }
        val backupBranch = buildGitService.updateRepository(build)
        try {
            storageService.updateBuild(build)
        } catch (e: Exception) {
            log.error("Update operation failed")
            buildGitService.resetRepositoryTo(build, backupBranch)
            cacheService.cleanGameCaches()
            throw e
        }
    }

    @Maintenance(MaintenanceService.Type.MANUAL)
    suspend fun updateManual() = run(Operation.MANUAL) {
        log.info("Server start manual update task")
        if (manualGitService.repositoryExist().not()) {
            manualGitService.cloneRepository()
        }
        manualGitService.updateRepository()
        log.info("Server successfully finished manual update task")
    }

    @Maintenance(MaintenanceService.Type.LAUNCHER)
    suspend fun updateLauncher(system: SystemType, file: FilePart, version: String, name: String?) =
        run(Operation.LAUNCHER) {
            log.info("Server start launcher update task")
            val tempFile = Files.createTempFile("mfr-launcher_", "_client")
            try {
                val launcherFolder = properties.launcherFolder.toPath().resolve(system.name)
                Files.createDirectories(launcherFolder)
                file.transferTo(tempFile).awaitFirstOrNull()
                launcherService.findBySystem(system)?.also { launcher ->
                    val launcherPath = launcherFolder.resolve(name ?: launcher.fileName)
                    Files.move(tempFile, launcherPath, StandardCopyOption.REPLACE_EXISTING)
                    launcher.version = version
                    launcher.md5 = launcherPath.also {
                        if (it.exists().not()) {
                            throw IllegalArgumentException("Launcher file doesn't exist")
                        }
                        launcher.size = it.fileSize()
                    }.md5()
                    launcher.fileName = name ?: launcher.fileName
                    launcherService.save(launcher)
                    log.info("Server successfully finished launcher update task")
                } ?: name?.also { fileName ->
                    val launcherPath = launcherFolder.resolve(fileName)
                    Files.move(tempFile, launcherPath, StandardCopyOption.REPLACE_EXISTING)
                    launcherPath.takeIf { it.exists() }
                        ?.also { path ->
                            Launcher(
                                system = system,
                                version = version,
                                fileName = fileName,
                                size = path.fileSize(),
                                md5 = path.md5()
                            ).also { launcherService.save(it) }
                            log.info("Server successfully finished upload new launcher task")
                        } ?: throw IllegalArgumentException("Launcher file doesn't exist")
                } ?: throw IllegalArgumentException("Launcher doesn't exist")
                streamService.updateLauncherVersion(system, version)
            } finally {
                tempFile.deleteIfExists()
            }
        }

    private suspend fun run(operation: Operation, function: suspend () -> Unit) {
        log.info("Try to start ${operation.type} update")
        val now = LocalDateTime.now()
        val result = currentTasks.getOrPut(operation) { now }
        if (result == now) {
            try {
                log.info("Update ${operation.type} has been successfully run")
                function()
                log.info("Update ${operation.type} successfully finished")
            } catch (e: Exception) {
                log.error("An error occurred during the update ${operation.type}", e)
                throw e
            } finally {
                currentTasks.remove(operation)
            }
        } else {
            log.info("Update ${operation.type} is already running at $result")
        }
    }

    private enum class Operation(val type: String) {
        GAME("Build"), MANUAL("Manual"), LAUNCHER("Launcher")
    }

    companion object {
        private val log by Logger()
    }
}