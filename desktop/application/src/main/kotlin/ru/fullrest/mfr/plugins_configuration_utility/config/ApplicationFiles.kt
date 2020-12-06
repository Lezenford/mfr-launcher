package ru.fullrest.mfr.plugins_configuration_utility.config

import org.springframework.context.annotation.Configuration
import ru.fullrest.mfr.plugins_configuration_utility.common.FileNameConstant
import ru.fullrest.mfr.plugins_configuration_utility.exception.StartApplicationException
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository
import java.io.File

@Configuration
class ApplicationFiles(
    private val properties: ApplicationProperties,
    private val propertiesRepository: PropertiesRepository
) : Loggable {
    lateinit var gameFolder: File
    lateinit var morrowind: File
    lateinit var openMw: File
    lateinit var launcher: File
    lateinit var openMwLauncher: File
    lateinit var mcp: File
    lateinit var mge: File
    lateinit var readme: File
    lateinit var dataFiles: File
    lateinit var optional: File
    lateinit var version: File
    lateinit var schema: File
    lateinit var mgeFolder: File
    lateinit var mgeConfig: File
    lateinit var mgeBackupConfig: File
    lateinit var highPerformanceMge: File
    lateinit var middlePerformanceMge: File
    lateinit var lowPerformanceMge: File
    lateinit var necroPerformanceMge: File
    lateinit var openMwConfigFolder: File
    lateinit var openMwBackupConfigFolder: File
    lateinit var highPerformanceOpenMwFolder: File
    lateinit var middlePerformanceOpenMwFolder: File
    lateinit var lowPerformanceOpenMwFolder: File
    lateinit var necroPerformanceOpenMwFolder: File

    fun checkInstall(): Boolean {
        val morrowindEsmFile = File(properties.gamePath + File.separator + properties.morrowindEsm)
        val tribunalEsmFile = File(properties.gamePath + File.separator + properties.tribunalEsm)
        val bloodmoonEsmFile = File(properties.gamePath + File.separator + properties.bloodmoonEsm)
        return morrowindEsmFile.exists() && tribunalEsmFile.exists() && bloodmoonEsmFile.exists()
    }

    /**
     * Init all files after game install
     */
    fun init() {
        propertiesRepository.findByKey(PropertyKey.INSTALLED)?.also {
            properties.initVersion()
            gameFolder = File(properties.gamePath)
            morrowind = createAndCheck(properties.morrowind, false)
            openMw = createAndCheck(properties.openMw, false)
            launcher = createAndCheck(properties.launcher, false)
            openMwLauncher = createAndCheck(properties.openMwLauncher, false)
            mcp = createAndCheck(properties.mcp, false)
            mge = createAndCheck(properties.mge, false)
            readme = createAndCheck(properties.readme, false)
            dataFiles = createAndCheck(properties.dataFiles, true)
            optional = File(properties.gamePath + File.separator + properties.optional)
            version = createAndCheck(properties.versionFileName, false)
            schema = createAndCheck(properties.schemaFileName, false)
            mgeFolder = createAndCheck(properties.mgeFolder, true)
            mgeConfig = File(properties.gamePath + File.separator + properties.mgeCurrentConfig)
            mgeBackupConfig = File(properties.gamePath + File.separator + properties.mgeBackupCurrentConfig)
            highPerformanceMge = createAndCheck(properties.highPerformanceMgeConfig, false)
            middlePerformanceMge = createAndCheck(properties.middlePerformanceMgeConfig, false)
            lowPerformanceMge = createAndCheck(properties.lowPerformanceMgeConfig, false)
            necroPerformanceMge = createAndCheck(properties.necroPerformanceMgeConfig, false)
            openMwConfigFolder =
                File(System.getProperty("user.home") + File.separator + properties.currentOpenMwConfigFolder) //Can be not exists
            openMwBackupConfigFolder =
                File(properties.gamePath + File.separator + properties.currentBackupOpenMwConfigFolder) //Can be not exists
            highPerformanceOpenMwFolder = createAndCheck(properties.highPerformanceOpenMwConfigFolder, true)
            middlePerformanceOpenMwFolder = createAndCheck(properties.middlePerformanceOpenMwConfigFolder, true)
            lowPerformanceOpenMwFolder = createAndCheck(properties.lowPerformanceMgeOpenMwConfigFolder, true)
            necroPerformanceOpenMwFolder = createAndCheck(properties.necroPerformanceOpenMwConfigFolder, true)
        } ?: throw StartApplicationException("Game must be installed before init game files")
    }

    fun updateEsmFileChangeDate() {
        FileNameConstant.esmFileList.forEach {
            try {
                File("${dataFiles.absolutePath}${File.separator}${it.first}").setLastModified(it.second)
            } catch (e: Exception) {
                log().error("Error to change last modified date in file ${it.first}", e)
            }
        }
    }

    private fun createAndCheck(path: String, isDirectory: Boolean): File {
        val file = File(properties.gamePath + File.separator + path)
        return if (file.exists() && file.isDirectory == isDirectory) {
            file
        } else {
            throw StartApplicationException("File ${file.absolutePath} incorrect")
        }
    }
}