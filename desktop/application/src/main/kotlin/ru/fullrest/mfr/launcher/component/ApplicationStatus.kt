package ru.fullrest.mfr.launcher.component

import org.springframework.stereotype.Component

@Component
class ApplicationStatus {
    val gameVersion: Observer<String> = Observer("")
    val gameInstalled: Observer<Boolean> = Observer(false)
    val gameBuildActive: Observer<Int> = Observer(0)
    val onlineMode: Observer<Boolean> = Observer(true)

    class Observer<T>(
        value: T
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
    }
}