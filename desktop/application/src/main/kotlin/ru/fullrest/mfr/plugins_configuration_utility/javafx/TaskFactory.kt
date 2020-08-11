package ru.fullrest.mfr.plugins_configuration_utility.javafx

import org.springframework.beans.factory.annotation.Lookup
import org.springframework.stereotype.Component
import ru.fullrest.mfr.plugins_configuration_utility.javafx.task.FillSchemeTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.task.GameInstallTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.task.LauncherUpdateTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.task.PluginTask

@Component
class TaskFactory {

    @Lookup
    fun getPluginTask(): PluginTask = Any() as PluginTask

    @Lookup
    fun getFillSchemeTask(): FillSchemeTask = Any() as FillSchemeTask

    @Lookup
    fun getGameInstallTask(): GameInstallTask = Any() as GameInstallTask

    @Lookup
    fun getLauncherUpdateTask(): LauncherUpdateTask = Any() as LauncherUpdateTask
}