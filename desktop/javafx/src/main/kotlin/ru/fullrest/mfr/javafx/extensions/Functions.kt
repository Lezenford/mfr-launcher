package ru.fullrest.mfr.javafx.extensions

import javafx.application.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking

/**
 * Выполнение действий с интерфейсом и интерфейсными элементами
 * должно происходить только в JavaFX Thread
 * В данном случае, если вызов происходит из другого потока,
 * он будет остановлен до получения результата
 */
fun <T> runFx(block: () -> T): T {
    return if (Platform.isFxApplicationThread()) {
        block()
    } else {
        runBlocking(Dispatchers.JavaFx) {
            block()
        }
    }
}