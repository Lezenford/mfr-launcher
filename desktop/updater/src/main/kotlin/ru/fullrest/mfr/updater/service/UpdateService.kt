package ru.fullrest.mfr.updater.service

import ru.fullrest.mfr.updater.common.Constant
import ru.fullrest.mfr.updater.logging.Loggable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess


class UpdateService : Loggable {

    fun update() {
        Thread.sleep(2000)
        val launcher = File(Constant.launcherFileName)
        val downloadedLauncher = File("${Constant.downloadFolder}${File.separator}${Constant.downloadedFileName}")
        if (downloadedLauncher.exists().not()) {
            log().error("Downloaded file doesn't exist")
            exitProcess(0)
        }
        val tempFile = File("old")
        try {
            loop@ for (i in 1..10) {
                try {
                    launcher.renameTo(tempFile)
                    break@loop
                } catch (e: Exception) {
                    if (i < 10) {
                        Thread.sleep(2000)
                    } else {
                        throw e
                    }
                }
            }
            ZipInputStream(FileInputStream(downloadedLauncher), Charset.defaultCharset()).use { inputStream ->
                var entry: ZipEntry? = inputStream.nextEntry
                while (entry != null) {
                    val fileName = entry.name
                    if (fileName == Constant.downloadedFileName) {
                        try {
                            FileOutputStream(launcher).use { outputStream ->
                                while (inputStream.available() > 0) {
                                    outputStream.write(inputStream.readNBytes(65536))
                                }
                            }
                        } catch (e: Exception) {
                            log().error(e.message)
                        }
                    }
                    entry = inputStream.nextEntry
                }
            }
            Files.deleteIfExists(tempFile.toPath())
            Files.deleteIfExists(downloadedLauncher.toPath())
        } catch (e: Exception) {
            log().error("Error to install new version", e)
            if (tempFile.exists()) {
                tempFile.renameTo(launcher)
            }
        }
        try {
            Runtime.getRuntime().exec("\"${launcher.absolutePath}\"", null, File("").absoluteFile)
        } catch (e: Exception) {
            log().error("Error to app start", e)
        }
    }
}