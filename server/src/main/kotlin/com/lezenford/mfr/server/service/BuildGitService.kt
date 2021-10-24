package com.lezenford.mfr.server.service

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.model.entity.Build
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.springframework.stereotype.Service
import java.io.File

@Service
class BuildGitService(
    override val transportConfigCallback: TransportConfigCallback,
    private val serverSettingProperties: ServerSettingProperties
) : GitService() {
    override val repositoryPath: File = File(serverSettingProperties.build.local)

    fun repositoryExist(build: Build): Boolean = build.repository().directory.exists()

    fun cloneRepository(build: Build) {
        cloneRepository(
            url = serverSettingProperties.build.remote,
            path = repositoryPath.resolve(build.branch),
            branch = build.branch
        )
    }

    fun updateRepository(build: Build): String = updateRepository(
        repository = build.repository(),
        branch = build.branch,
        backupPrefix = build.name
    )

    fun resetRepositoryTo(build: Build, branch: String) {
        resetRepositoryTo(repository = build.repository(), branch = branch)
    }

    private fun Build.repository(): Repository =
        FileRepositoryBuilder.create(repositoryPath.resolve(branch).resolve(GIT_FOLDER))
}