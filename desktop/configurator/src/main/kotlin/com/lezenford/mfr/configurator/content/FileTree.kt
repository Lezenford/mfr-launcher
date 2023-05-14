package com.lezenford.mfr.configurator.content

import com.lezenford.mfr.common.extensions.toPath
import java.nio.file.Path
import java.util.LinkedList

class FileTree(
    val root: GameFolder,
    private val map: MutableMap<Int, File> = hashMapOf()
) : Findable, Map<Int, File> by map {
    private val pathMap = hashMapOf<Path, File>()

    init {
        val stack = LinkedList<GameFolder>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val first = stack.removeFirst()
            map[first.id] = first
            pathMap[first.relativePath] = first
            first.child.onEach {
                if (it is GameFolder) {
                    stack.addLast(it)
                } else {
                    map[it.id] = it
                    pathMap[it.relativePath] = it
                }
            }
        }
    }

    override fun find(path: Path) = pathMap[path]

    fun refresh() {
        map.forEach { (_, value) -> if (value.ignore) value.ignore = false }
    }
}