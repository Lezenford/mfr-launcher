package com.lezenford.mfr.configurator.service

import com.lezenford.mfr.common.extensions.toPath
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

@Service
class IgnoreService(
    private val rootFolderReference: AtomicReference<Path>
) {
    private val rootFolder: Path
        get() = rootFolderReference.get()
            ?: throw IllegalArgumentException("Root folder didn't init")

    private val ignoreFile: Path
        get() = rootFolder.resolve(IGNORE_FILE).also {
            if (!it.exists()) it.createFile()
        }
    val ignoreFiles = mutableSetOf<Path>()
        get() {
            if (field.isEmpty()) ignoreFile.readLines().forEach { field.add(it.toPath()) }
            return field
        }

    fun addToIgnore(list: List<Path>) {
        list.forEach { ignoreFiles.add(it) }
    }

    fun removeFromIgnore(list: List<Path>) {
        list.forEach { ignoreFiles.remove(it) }
    }

    fun saveIgnoreFile() {
        ignoreFile.writeLines(ignoreFiles.map { it.toString() })
    }

    companion object {
        const val IGNORE_FILE = ".configuratorignore"
    }
}