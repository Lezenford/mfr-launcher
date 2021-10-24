package com.lezenford.mfr.server.service

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Repository
import ru.fullrest.mfr.common.extensions.Logger
import java.io.File
import java.util.*

abstract class GitService {
    protected abstract val transportConfigCallback: TransportConfigCallback
    protected abstract val repositoryPath: File

    protected fun cloneRepository(url: String, path: File, branch: String) {
        log.info("Clone repository operation started for $path branch: $branch")
        Git.cloneRepository()
            .setURI(url)
            .setDirectory(path.also { it.mkdirs() })
            .setBranchesToClone(listOf("refs/heads/$branch"))
            .setBranch("refs/heads/$branch")
            .setTransportConfigCallback(transportConfigCallback)
            .call()
        log.info("Clone repository successfully finished")
    }

    protected fun updateRepository(repository: Repository, branch: String, backupPrefix: String = ""): String {
        log.info("Update repository operation started for ${repository.directory.absolutePath} branch: $branch")
        val backupBranch = listOf("backup", backupPrefix, Date().time.toString())
            .filter { it.isNotBlank() }.joinToString("_")
        Git(repository).apply {
            branchCreate()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK)
                .setName(backupBranch)
                .call()
            log.info("Created backup branch $backupBranch")
            checkout()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setName(branch)
                .setForced(true)
                .call()
            pull()
                .setTransportConfigCallback(transportConfigCallback)
                .call()
        }
        log.info("Update repository successfully finished")
        return backupBranch
    }

    fun resetRepositoryTo(repository: Repository, branch: String) {
        log.info("Restore ${repository.directory.absolutePath} to branch $branch")
        Git(repository).apply {
            checkout()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK)
                .setName(branch)
                .setForced(true)
                .call()
        }
        log.info("Build successfully restored")
    }

    companion object {
        const val GIT_FOLDER = ".git"
        private val log by Logger()
    }
}