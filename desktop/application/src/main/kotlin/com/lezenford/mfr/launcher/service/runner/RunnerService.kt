package com.lezenford.mfr.launcher.service.runner

import com.lezenford.mfr.launcher.Launcher
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.service.State
import kotlin.io.path.pathString


abstract class RunnerService {
    protected abstract val applicationProperties: ApplicationProperties
    protected abstract val application: Launcher

    abstract fun startClassicGame()
    abstract fun startClassicLauncher()
    abstract fun startMge(): Process
    abstract fun startMcp()
    abstract fun startOpenMwGame()
    abstract fun startOpenMwLauncher()

    fun openReadme() {
        when (State.onlineMode.value) {
            true -> applicationProperties.readme.remote
            false -> applicationProperties.readme.local.pathString
        }.also {
            application.hostServices.showDocument(it)
        }
    }

    fun openForum() {
        application.hostServices.showDocument(applicationProperties.social.forum)
    }

    fun openDiscord() {
        application.hostServices.showDocument(applicationProperties.social.discord)
    }

    fun openYoutube() {
        application.hostServices.showDocument(applicationProperties.social.youtube)
    }

    fun openVk() {
        application.hostServices.showDocument(applicationProperties.social.vk)
    }

    fun openPatreon() {
        application.hostServices.showDocument(applicationProperties.social.patreon)
    }
}