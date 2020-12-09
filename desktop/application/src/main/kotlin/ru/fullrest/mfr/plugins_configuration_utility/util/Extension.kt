package ru.fullrest.mfr.plugins_configuration_utility.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Files
import java.util.*

suspend fun <T, V> parallelCalculation(
    items: List<T>,
    action: (T) -> V
): List<V> {
    val exceptionHandler = CoroutineExceptionHandler { _, e -> LogManager.getLogger(Extension::javaClass).error(e) }
    return items.map { CoroutineScope(Dispatchers.Default).async(exceptionHandler) { action(it) } }.map { it.await() }
}

fun File.ifNotExists(block: () -> (Unit)) {
    if (this.exists().not()) {
        block()
    }
}

fun File.listAllFiles(): List<File> {
    val result = mutableListOf<File>()
    Files.walk(toPath()).sorted(Comparator.naturalOrder())
        .forEach { path ->
            val file = path.toFile()
            if (file.isFile) {
                result.add(file)
            }
        }
    return result
}

class Extension