package com.lezenford.mfr.server.service.git

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.springframework.stereotype.Service
import java.io.File

@Service
class ManualGitService(
    override val transportConfigCallback: TransportConfigCallback,
    private val serverSettingProperties: ServerSettingProperties
) : GitService() {
    override val repositoryPath: File = File(serverSettingProperties.manual.local)

    fun repositoryExist(): Boolean = repositoryPath.exists()

    fun cloneRepository() {
        cloneRepository(
            url = serverSettingProperties.manual.remote,
            path = repositoryPath,
            branch = MASTER
        )
    }

    fun updateRepository() {
        updateRepository(
            repository = FileRepositoryBuilder.create(repositoryPath.resolve(GIT_FOLDER)),
            branch = MASTER
        )
    }

    companion object {
        private const val MASTER = "master"
    }
}