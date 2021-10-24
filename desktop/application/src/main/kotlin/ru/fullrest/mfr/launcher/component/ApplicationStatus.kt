package ru.fullrest.mfr.launcher.component

import org.springframework.stereotype.Component
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.config.properties.GameProperties
import kotlin.io.path.exists
import kotlin.io.path.readLines

@Component
class ApplicationStatus(
    private val applicationProperties: ApplicationProperties,
    private val gameProperties: GameProperties
) {
    val gameVersion: Observer<String> = Observer("") {
        value = gameProperties.versionFile.takeIf { it.exists() }?.readLines()?.find { line -> line.isNotEmpty() } ?: ""
    }
    val gameInstalled: Observer<Boolean> = Observer(false)
    val gameBuildActive: Observer<Int> = Observer(0)
    val onlineMode: Observer<Boolean> = Observer(true)

    class Observer<T>(
        value: T,
        private val updateFunction: Observer<T>.() -> Unit = {}
    ) {
        private val listeners: MutableList<(T) -> Unit> = mutableListOf()
        var value = value
            set(value) {
                field = value
                listeners.forEach { it(value) }
            }

        fun addListener(event: (T) -> Unit) {
            listeners.add(event)
        }

        fun update() {
            updateFunction()
        }
    }
}