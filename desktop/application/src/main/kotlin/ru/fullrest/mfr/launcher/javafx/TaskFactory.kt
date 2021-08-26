package ru.fullrest.mfr.launcher.javafx

import org.springframework.beans.factory.annotation.Lookup
import org.springframework.stereotype.Component
import ru.fullrest.mfr.launcher.javafx.task.ApplyOptionsTask
import ru.fullrest.mfr.launcher.javafx.task.CheckGameConsistencyTask
import ru.fullrest.mfr.launcher.javafx.task.CheckGameUpdateTask
import ru.fullrest.mfr.launcher.javafx.task.FileDownloadTask
import ru.fullrest.mfr.launcher.javafx.task.FillSchemeTask
import ru.fullrest.mfr.launcher.javafx.task.GameInstallTask
import ru.fullrest.mfr.launcher.javafx.task.GameUpdateTask
import ru.fullrest.mfr.launcher.javafx.task.LauncherUpdateTask

@Component
class TaskFactory {

    @Lookup
    fun gameInstallTask(): GameInstallTask = Any() as GameInstallTask

    @Lookup
    fun fileDownloadTask(): FileDownloadTask = Any() as FileDownloadTask

    @Lookup
    fun applyOptionsTask(): ApplyOptionsTask = Any() as ApplyOptionsTask

    @Lookup
    fun fillSchemaTask(): FillSchemeTask = Any() as FillSchemeTask

    @Lookup
    fun checkGameUpdateTask(): CheckGameUpdateTask = Any() as CheckGameUpdateTask

    @Lookup
    fun gameUpdateTask(): GameUpdateTask = Any() as GameUpdateTask

    @Lookup
    fun checkGameConsistencyTask(): CheckGameConsistencyTask = Any() as CheckGameConsistencyTask

    @Lookup
    fun launcherUpdateTask(): LauncherUpdateTask = Any() as LauncherUpdateTask
}