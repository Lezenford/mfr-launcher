package ru.fullrest.mfr.plugins_configuration_utility.service

import com.fasterxml.jackson.databind.ObjectMapper
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.springframework.stereotype.Component
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@Component
class FileService(
    private val propertiesConfiguration: ApplicationProperties,
    private val files: ApplicationFiles,
    private val mapper: ObjectMapper
) : Loggable {
    fun openDirectoryChooser(folder: File, stage: Stage): File =
        DirectoryChooser().let {
            it.initialDirectory = folder
            it.showDialog(stage)
        }

    fun openFilesChooser(folder: File, stage: Stage): List<File> =
        FileChooser().let {
            it.initialDirectory = folder
            it.showOpenMultipleDialog(stage)
        }

    fun openFileChooser(folder: File, stage: Stage): File =
        FileChooser().let {
            it.initialDirectory = folder
            it.showOpenDialog(stage)
        }

    fun openSaveFileChooser(folder: File, fileName: String, stage: Stage): File =
        FileChooser().let {
            it.initialDirectory = folder
            it.initialFileName = fileName
            it.showSaveDialog(stage)
        }

    fun readFile(file: File): String {
        val builder = StringBuilder()
        try {
            BufferedReader(FileReader(file, StandardCharsets.UTF_8)).use { reader ->
                while (reader.ready()) {
                    builder.append(reader.readLine()).append("\n")
                }
            }
        } catch (e: IOException) {
            log().error("Can't read file!", e)
        }
        return builder.toString()
    }

    fun getFileMD5(file: File?): ByteArray? {
        if (file != null) {
            if (file.exists()) {
                if (file.isFile) {
                    try {
                        BufferedInputStream(FileInputStream(file)).use { inputStream ->
                            return MessageDigest.getInstance(MD5).also { it.update(inputStream.readAllBytes()) }
                                .digest()
                        }
                    } catch (e: IOException) {
                        log().error("File MD5 check error!\n", e)
                    } catch (e: NoSuchAlgorithmException) {
                        log().error("File MD5 check error!\n", e)
                    }
                } else {
                    log().error("File is not a file: ${file.absolutePath}")
                }
            } else {
                log().error("File does not exist: ${file.absolutePath}")
            }
        } else {
            log().error("File is null")
        }
        return null
    }

    fun removeFromGameDirectory(details: Details) {
        if (!Files.isWritable(Paths.get(files.gameFolder.path + File.separator + details.gamePath))) {
            log().error("File does not exists: ${details.gamePath}")
        }
        try {
            Files.deleteIfExists(Paths.get(files.gameFolder.path + File.separator + details.gamePath))
        } catch (e: IOException) {
            log().error("Error deleting file.\n", e)
        }
    }

    fun copyToGameDirectory(details: Details) {
        try {
            val file = File(files.gameFolder.path + File.separator + details.gamePath)
            if (!file.parentFile.exists()) {
                if (!file.parentFile.mkdirs() && Files.notExists(file.toPath().parent)) {
                    log().error("Can't create directory " + file.toPath().parent.toAbsolutePath())
                    return
                }
            }
            Files.copy(
                Paths.get(files.optional.path + File.separator + details.storagePath),
                Paths.get(files.gameFolder.path + File.separator + details.gamePath),
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: IOException) {
            log().error("Error coping to game directory.", e)
        }
    }

    fun saveMGEBackup() {
        try {
            if (files.mgeBackupConfig.parentFile.exists().not()) {
                files.mgeBackupConfig.parentFile.mkdirs()
            }
            Files.copy(
                files.mgeConfig.toPath(),
                files.mgeBackupConfig.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: IOException) {
            log().error("Can't copy file.\n ", e)
        }
    }

    fun setMGEConfig(source: File) {
        if (source.exists()) {
            try {
                Files.copy(source.toPath(), files.mgeConfig.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                log().error("Can't copy file.\n", e)
            }
        } else {
            log().error("MGE.ini doesn't exist")
        }
    }

    fun saveOpenMwBackupConfig() {
        try {
            files.openMwConfigFolder.listFiles()?.also {
                val currentConfigFiles = it.toList()
                for (s in propertiesConfiguration.openMwConfigFiles) {
                    val source = currentConfigFiles.stream().filter { file: File ->
                        file.name.equals(s, ignoreCase = true)
                    }.findAny().orElseThrow { IOException("File doesn't exist: $s") }.toPath()
                    val openMwBackupConfigFolder = files.openMwBackupConfigFolder
                    if (!openMwBackupConfigFolder.exists()) {
                        val mkdirs = openMwBackupConfigFolder.mkdirs()
                        if (!mkdirs) {
                            log().error("Can't create backup folder")
                        }
                    }
                    val target = Paths.get(openMwBackupConfigFolder.absolutePath + File.separator + s)
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } ?: throw IOException("Configuration not found")
        } catch (e: IOException) {
            log().error("Can't copy file.\n", e)
        }
    }

    fun setOpenMwConfig(folder: File) {
        try {
            folder.listFiles()?.also { configFilesArray ->
                val currentConfigFiles = listOf(*configFilesArray)
                for (s in propertiesConfiguration.openMwConfigFiles) {
                    val source = currentConfigFiles.stream().filter { file: File ->
                        file.name.equals(s, ignoreCase = true)
                    }.findAny().orElseThrow { IOException("File doesn't exist: $s") }.toPath()
                    val target =
                        Paths.get(files.openMwConfigFolder.absolutePath + File.separator + s).also {
                            if (Files.exists(it.parent).not()) {
                                it.parent.toFile().mkdirs()
                            }
                        }
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } ?: throw IOException("Configuration not found")
        } catch (e: IOException) {
            log().error("Can't copy file.\n", e)
        }
    }

    fun prepareOpenMwConfigFiles() {
        val folders = listOf(
            files.highPerformanceOpenMwFolder,
            files.middlePerformanceOpenMwFolder,
            files.lowPerformanceOpenMwFolder,
            files.necroPerformanceOpenMwFolder
        )
        for (folder in folders) {
            val source = File(folder.absolutePath + File.separator + "openmw_template.cfg")
            val target = File(folder.absolutePath + File.separator + "openmw.cfg")
            if (source.exists()) {
                try {
                    val builder = StringBuilder()
                    BufferedReader(FileReader(source)).use { reader ->
                        while (reader.ready()) {
                            builder.append(reader.readLine()).append(System.lineSeparator())
                        }
                    }
                    BufferedWriter(FileWriter(target)).use { writer ->
                        val result = builder.toString().replace(
                            oldValue = propertiesConfiguration.openMwPathChangeValue,
                            newValue = files.dataFiles.absolutePath
                        )
                        writer.write(result)
                    }
                } catch (e: IOException) {
                    log().error("Can't update openmw config", e)
                }
            } else {
                log().error("Can't find file for change game path. " + source.absolutePath)
            }
        }
    }

    fun createSchemaFile(
        groups: List<Group?>,
        file: File?
    ) {
        try {
            BufferedWriter(FileWriter(file, StandardCharsets.UTF_8)).use { writer ->
                for (group in groups) {
                    val value = mapper.writeValueAsString(group)
                    writer.write("$value\n")
                }
            }
        } catch (e: IOException) {
            log().error("Error creating new schema file", e)
        }
    }

    companion object {
        private const val MD5 = "MD5"
    }
}