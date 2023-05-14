package com.lezenford.mfr.configurator.service

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.file.CONTENT_FILE_NAME
import com.lezenford.mfr.common.protocol.file.SCHEMA_FILE_NAME
import com.lezenford.mfr.configurator.content.FileTree
import com.lezenford.mfr.configurator.content.GameFile
import com.lezenford.mfr.configurator.content.GameFolder
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolute

@Service
class FileTreeService(
    private val fileTreeReference: AtomicReference<FileTree>,
    private val rootFolderReference: AtomicReference<Path>,
    private val ignoreService: IgnoreService
) {
    val tree: FileTree get() = fileTreeReference.get().also { it.refresh(); applyIgnoreList() }

    private val ignoreFiles
        get() = ignoreService.ignoreFiles
            .plus(EXCLUDE_LIST.map { it.toPath() })
    private val rootFolder: Path
        get() = rootFolderReference.get()
            ?: throw IllegalArgumentException("Root folder didn't init")

    fun initRoot(rootFolder: Path) {
        rootFolderReference.set(rootFolder)
        val root = GameFolder(rootFolder)
        Files.walkFileTree(root.absolutePath, object : FileVisitor<Path> {
            private val stack = LinkedList<GameFolder>().also { it.add(root) }
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
                if (root.absolutePath.relativize(dir.absolute()).toString() in EXCLUDE_LIST) {
                    return FileVisitResult.SKIP_SUBTREE
                }
                if (dir.absolute() != rootFolder) {
                    stack.addLast(GameFolder(dir.toAbsolutePath(), rootFolder, stack.last))
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                if (root.absolutePath.relativize(file.absolute()).toString() in EXCLUDE_LIST) {
                    return FileVisitResult.SKIP_SUBTREE
                }
                stack.last.child.add(GameFile(rootFolder, file.toAbsolutePath(), stack.last))
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                log.error("Can't visit file ${file.toAbsolutePath()}")
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                stack.removeLast()
                return FileVisitResult.CONTINUE
            }
        })
        fileTreeReference.set(FileTree(root))
    }

    private fun applyIgnoreList() {
        fileTreeReference.get()?.also { root ->
            ignoreFiles.forEach { ignore ->
                root.find(ignore)?.ignore = true
            }
        }
    }

    companion object {
        private val log by Logger()
        private val EXCLUDE_LIST =
            setOf(".git", ".gitignore", ".idea", IgnoreService.IGNORE_FILE, CONTENT_FILE_NAME, SCHEMA_FILE_NAME)
    }
}