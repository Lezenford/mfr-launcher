package com.lezenford.mfr.configurator.content

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import java.nio.file.Path
import java.util.Collections
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap

class MainContent : Content {
    override val name: ObjectProperty<String> = SimpleObjectProperty("MAIN")
    override val files: MutableSet<GameFile> = Collections.synchronizedSet(hashSetOf())
}

class AdditionalContent(
    name: String,
    override var description: String = "",
    override var image: GameFile? = null
) : DownloadableContent {
    override val name: ObjectProperty<String> = SimpleObjectProperty(name)
    override val files: MutableSet<GameFile> = Collections.synchronizedSet(hashSetOf())
}

class SwitchableContent(
    name: String
) : Content {
    override val name: ObjectProperty<String> = SimpleObjectProperty(name)
    override val files: MutableSet<GameFile>
        get() = Collections.unmodifiableSet(options.flatMapTo(hashSetOf()) { it.files })

    val options: MutableSet<Option> = TreeSet()

    class Option(
        private val downloadableContent: DownloadableContent
    ) : DownloadableContent by downloadableContent {
        val alternativePaths: MutableMap<Int, Path> = ConcurrentHashMap()

        fun alternativePath(file: File): Path = alternativePaths[file.id]
            ?: throw IllegalArgumentException("Can't find alternative path for file ${file.relativePath} in option ${this.name}")
    }
}

interface DownloadableContent : Content {
    var description: String
    var image: GameFile?
}

interface Content : Comparable<Content> {
    val name: ObjectProperty<String>
    val files: MutableSet<GameFile>

    override fun compareTo(other: Content): Int {
        return name.value.compareTo(other.name.value)
    }
}