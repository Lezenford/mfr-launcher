package ru.fullrest.mfr.updater

import org.apache.logging.log4j.LogManager
import ru.fullrest.mfr.updater.service.UpdateService

class PluginConfigurationUtilityUpdater

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        LogManager.getLogger(PluginConfigurationUtilityUpdater::class.java).error(e)
    }
    UpdateService().update()
}