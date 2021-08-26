package ru.fullrest.mfr.launcher.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path
import kotlin.io.path.exists

@ConstructorBinding
@ConfigurationProperties(prefix = "game")
data class GameProperties(
    val optional: Path,
    val classic: Classic,
    val openMw: OpenMw,
    val versionFile: Path
) {
    data class Classic(
        val application: Path,
        val launcher: Path,
        val mcp: Path,
        val mge: Mge
    ) {
        fun exists(): Boolean {
            return application.exists() && launcher.exists() && mcp.exists() && mge.application.exists()
        }

        data class Mge(
            val application: Path,
            val folder: Path,
            val config: Path,
            val configBackup: Path,
            val templates: Templates
        )
    }

    data class OpenMw(
        val application: Path,
        val launcher: Path,
        val configFolder: Path,
        val configBackupFolder: Path,
        val configChangeValue: String,
        val templates: Templates
    ) {
        fun exists(): Boolean {
            return application.exists() && launcher.exists()
        }
    }

    data class Templates(
        val high: Path,
        val middle: Path,
        val low: Path,
        val basic: Path
    )
}

