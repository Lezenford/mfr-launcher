package com.lezenford.mfr.configurator.content

import java.nio.file.Path
import java.util.TreeSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.name
import kotlin.io.path.relativeTo

class GameFile(
    override val rootFolder: Path,
    override val absolutePath: Path,
    override val parent: GameFolder? = null
) : File(), Comparable<GameFile> {
    override fun compareTo(other: GameFile): Int = id.compareTo(other.id)
}

class GameFolder(
    override val absolutePath: Path,
    override val rootFolder: Path = absolutePath,
    override val parent: GameFolder? = null
) : File() {
    val child = TreeSet(
        Comparator.comparing<File, Int> { compareMap.getOrDefault(it::class, 3) }
            .thenComparing { o1, o2 -> o1.name.compareTo(o2.name) }
    )

    init {
        parent?.also { parent.child.add(this) }
    }

    override fun find(path: Path): File? {
        return if (path.isAbsolute) {
            if (absolutePath == path) {
                this
            } else {
                child.find { it.name == absolutePath.relativize(path).first().name }?.find(path)
            }
        } else {
            if (relativePath == path) {
                this
            } else {
                child.find { it.name == relativePath.relativize(path).first().name }?.find(path)
            }
        }
    }

    companion object {
        private val compareMap = mutableMapOf(GameFolder::class to 1, GameFile::class to 2)
    }
}

sealed class File : Findable {
    val id = counter.getAndIncrement()
    abstract val absolutePath: Path
    abstract val parent: GameFolder?
    protected abstract val rootFolder: Path
    val relativePath: Path by lazy { absolutePath.relativeTo(rootFolder) }
    val name: String by lazy { absolutePath.fileName.name }
    var ignore: Boolean = false
        set(value) {
            field = value
            if (parent?.findParentIgnore() == false) {
                updateHidden(ignore)
                var currentParent = parent
                while (currentParent != null) {
                    currentParent._hidden = currentParent.child.all { it.hidden }
                    currentParent = currentParent.parent
                }
            }
        }

    @Suppress("PropertyName")
    protected var _hidden: Boolean = ignore
    val hidden get() = _hidden

    private fun File.updateHidden(value: Boolean): Boolean {
        _hidden = when (this) {
            is GameFolder -> {
                child.onEach { it.updateHidden(ignore || value) }.all { it.hidden }
            }

            is GameFile -> {
                ignore || value
            }
        }
        return hidden
    }

    private fun File.findParentIgnore(): Boolean {
        return ignore || parent?.findParentIgnore() ?: false
    }

    override fun find(path: Path): File? {
        return if (path.isAbsolute) takeIf { path == absolutePath } else takeIf { path == relativePath }
    }

    override fun toString(): String {
        return "$name $relativePath"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        private val counter = AtomicInteger()
    }
}

interface Findable {
    fun find(path: Path): File?
}
