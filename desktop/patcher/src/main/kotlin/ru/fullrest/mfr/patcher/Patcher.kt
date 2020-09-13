package ru.fullrest.mfr.patcher

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import ru.fullrest.mfr.api.GameUpdate
import ru.fullrest.mfr.api.MoveFile
import java.io.*
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

fun main() {
    test()
    return
    val path = "C:\\Games\\morrowind-fullrest-repack(git)"
    val repository = FileRepositoryBuilder().setGitDir(File("$path\\.git")).build()
    val git = Git(repository)
    val branch =
        git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().find { it.name == "refs/heads/master" }
    val commit = RevWalk(repository).parseCommit(branch!!.objectId)
    val prev = commit.getParent(0)
    val diffFormatter = DiffFormatter(System.out)
    diffFormatter.setRepository(repository)
    val scan = diffFormatter.scan(commit, prev)
    val version = FileReader(File(path + File.separator + "Optional/version")).use { it.readText() }
    val addFiles = mutableListOf<String>()
    val removeFiles = mutableListOf<String>()
    val moveFiles = mutableListOf<MoveFile>()
    val gameUpdate =
        GameUpdate(version = version, addFiles = addFiles, moveFiles = moveFiles, removeFiles = removeFiles)
    scan.forEach {
        if (it.newPath != "mge3/MGE.ini") {
            when (it.changeType) {
                DiffEntry.ChangeType.ADD -> addFiles.add(it.newPath)
                DiffEntry.ChangeType.DELETE -> removeFiles.add(it.oldPath)
                DiffEntry.ChangeType.MODIFY -> addFiles.add(it.newPath)
                DiffEntry.ChangeType.RENAME -> moveFiles.add(MoveFile(it.oldPath, it.newPath))
                DiffEntry.ChangeType.COPY -> addFiles.add(it.newPath)
                else -> println(it.newPath)
            }
        }
    }
    val tempDirectory = Files.createTempDirectory("mfr_patch")
    val updateFile = File("${tempDirectory.toFile().absolutePath}${File.separator}${GameUpdate.FILE_NAME}")
    FileWriter(updateFile).use { it.write(ObjectMapper().writeValueAsString(gameUpdate)) }
    val patchFile = File("${tempDirectory.toFile().absolutePath}${File.separator}patch")
    addFiles.forEach {
        val source = File("$path${File.separator}$it")
        val target = File("${patchFile.absolutePath}${File.separator}$it")
        if (target.parentFile.exists().not()) {
            target.parentFile.mkdirs()
        }
        Files.copy(source.toPath(), target.toPath())
    }
    val archive = File(version)
    ZipOutputStream(FileOutputStream(archive)).use { zipOutputStream ->
        tempDirectory.toFile().listAllFiles().forEach {
            zipOutputStream.putNextEntry(ZipEntry(it.absolutePath.removePrefix("${tempDirectory.toFile().absolutePath}${File.separator}")))
            zipOutputStream.write(FileInputStream(it).use { fileInputStream -> fileInputStream.readAllBytes() })
            zipOutputStream.closeEntry()
        }
    }
    tempDirectory.toFile().deleteRecursively()
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

fun test() {
    val file: File = File("C:\\Users\\Famaly\\Dropbox\\Java\\mfr-pcu\\4.0.01")
    println(file.absolutePath)
    if (file.exists() && !file.isDirectory) {
        var version: String? = null
        var hasSchema = false
        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val zipEntry = entries.nextElement()
                if (!zipEntry.isDirectory && (zipEntry.name.toLowerCase() == "patch/optional/version" || zipEntry.name.toLowerCase() == "patch\\optional\\version")) {
                    zipFile.getInputStream(zipEntry).use { inputStream -> version = String(inputStream.readAllBytes()) }
                }
                if (!zipEntry.isDirectory && zipEntry.name == GameUpdate.FILE_NAME) {
                    hasSchema = true
                }
            }
        } catch (e: IOException) {
            println(e)
        }
        if (version != null && hasSchema) {

        } else {
            println("error")
        }
    } else {
        println("too")
    }
}