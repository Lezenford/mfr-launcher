package com.lezenford.mfr.launcher.service.factory

import com.lezenford.mfr.launcher.service.initiator.InitApplicationInitiator
import com.lezenford.mfr.launcher.task.ApplyOptionsTask
import com.lezenford.mfr.launcher.task.CheckGameConsistencyTask
import com.lezenford.mfr.launcher.task.DownloadFileTask
import com.lezenford.mfr.launcher.task.FillSchemeTask
import com.lezenford.mfr.launcher.task.GameInstallTask
import com.lezenford.mfr.launcher.task.GameUpdateTask
import com.lezenford.mfr.launcher.task.LauncherUpdateTask
import org.springframework.beans.factory.annotation.Lookup
import org.springframework.stereotype.Component

@Component
class TaskFactory {

    @Lookup
    fun gameInstallTask(): GameInstallTask = lookup()

    @Lookup
    fun downloadFileTask(): DownloadFileTask = lookup()

    @Lookup
    fun applyOptionsTask(): ApplyOptionsTask = lookup()

    @Lookup
    fun fillSchemaTask(): FillSchemeTask = lookup()

    @Lookup
    fun gameUpdateTask(): GameUpdateTask = lookup()

    @Lookup
    fun checkGameConsistencyTask(): CheckGameConsistencyTask = lookup()

    @Lookup
    fun launcherUpdateTask(): LauncherUpdateTask = lookup()

    @Lookup
    fun initTask(): InitApplicationInitiator = lookup()

    private inline fun <reified T> lookup(): T = Any() as T
}