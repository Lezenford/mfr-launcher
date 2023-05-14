package com.lezenford.mfr.launcher.config

import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories

@Configuration
class PropertiesBeanPostProcessor(
    private val applicationContext: ApplicationContext
) : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        if (bean is ApplicationProperties) {
            val gameFolder = bean.gameFolder.toAbsolutePath()
            return bean.copy(
                gameFolder = gameFolder,
                readme = bean.readme.copy(
                    local = gameFolder.absolute().resolve(bean.readme.local)
                ),
            )
        }
        if (bean is GameProperties) {
            val gameFolder = applicationContext.getBean(ApplicationProperties::class.java).gameFolder.toAbsolutePath()
            fun Path.resolve() = gameFolder.resolve(this)
            return bean.run {
                GameProperties(
                    optional = optional.resolve(),
                    versionFile = versionFile.resolve(),
                    classic = classic.run {
                        GameProperties.Classic(
                            application = application.resolve(),
                            launcher = launcher.resolve(),
                            mcp = mcp.resolve(),
                            mge = mge.run {
                                GameProperties.Classic.Mge(
                                    application = application.resolve(),
                                    config = config.resolve(),
                                    configBackup = configBackup.resolve(),
                                    templates = templates.run {
                                        GameProperties.Templates(
                                            high = high.resolve(),
                                            middle = middle.resolve(),
                                            low = low.resolve(),
                                            basic = basic.resolve()
                                        )
                                    },
                                )
                            },
                        )
                    },
                    openMw = openMw.run {
                        GameProperties.OpenMw(
                            application = application.resolve(),
                            launcher = launcher.resolve(),
                            configFolder = FileSystemView.getFileSystemView().defaultDirectory.toPath()
                                .resolve(openMw.configFolder)
                                .apply { createDirectories() },
                            configBackupFolder = configBackupFolder.resolve(),
                            configChangeValue = configChangeValue,
                            templates = templates.run {
                                GameProperties.Templates(
                                    high = high.resolve(),
                                    middle = middle.resolve(),
                                    low = low.resolve(),
                                    basic = basic.resolve()
                                )
                            }
                        )
                    }
                )
            }
        }
        return super.postProcessAfterInitialization(bean, beanName)
    }
}