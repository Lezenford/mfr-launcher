package com.lezenford.mfr.server.service

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.model.entity.Build
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.springframework.stereotype.Service
import ru.fullrest.mfr.common.extensions.Logger
import java.io.File
import java.util.*

@Service
class GitService(
    private val transportConfigCallback: TransportConfigCallback,
    private val serverSettingProperties: ServerSettingProperties
) {
    private val repositoryPrefix: File = File(serverSettingProperties.buildFolder)

    fun repositoryExist(build: Build): Boolean = build.repository().directory.exists()

    fun cloneRepository(build: Build) {
        log.info("Clone repository operation started for build ${build.name}")
        Git.cloneRepository()
            .setURI(serverSettingProperties.git.url)
            .setDirectory(repositoryPrefix.resolve(build.branch).also { it.mkdirs() })
            .setBranchesToClone(listOf("refs/heads/${build.branch}"))
            .setBranch("refs/heads/${build.branch}")
            .setTransportConfigCallback(transportConfigCallback)
            .call()
        log.info("Clone repository successfully finished")
    }

    fun updateRepository(build: Build): String {
        log.info("Update repository operation started for build ${build.name}")
        val backupBranch = "backup_${build.name}_${Date().time}"
        Git(build.repository()).apply {
            branchCreate()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK)
                .setName(backupBranch)
                .call()
            log.info("Created backup branch $backupBranch")
            checkout()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setName(build.branch)
                .setForced(true)
                .call()
            pull()
                .setTransportConfigCallback(transportConfigCallback)
                .call()
        }
        log.info("Update repository successfully finished")
        return backupBranch;
    }

    fun resetRepositoryTo(build: Build, branch: String) {
        log.info("Restore build ${build.name} to branch $branch")
        Git(build.repository()).apply {
            checkout()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK)
                .setName(branch)
                .setForced(true)
                .call()
        }
        log.info("Build successfully restored")
    }

    private fun Build.repository(): Repository =
        FileRepositoryBuilder.create(repositoryPrefix.resolve(branch).resolve(GIT_FOLDER))

    companion object {
        private val log by Logger()
        private const val GIT_FOLDER = ".git"
    }
}