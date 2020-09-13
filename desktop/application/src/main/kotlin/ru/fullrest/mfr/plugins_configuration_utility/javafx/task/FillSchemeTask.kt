package ru.fullrest.mfr.plugins_configuration_utility.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.stage.Stage
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.EmbeddedProgressController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.createProgressWindow
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository
import ru.fullrest.mfr.plugins_configuration_utility.service.FileService
import ru.fullrest.mfr.plugins_configuration_utility.service.GroupService
import ru.fullrest.mfr.plugins_configuration_utility.service.RestTemplateService
import ru.fullrest.mfr.plugins_configuration_utility.util.parallelCalculation
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class FillSchemeTask(
    private val groupService: GroupService,
    private val files: ApplicationFiles,
    private val fileService: FileService,
    private val propertiesRepository: PropertiesRepository,
    private val detailsRepository: DetailsRepository,
    private val mapper: ObjectMapper,
    restTemplateService: RestTemplateService,
    applicationProperties: ApplicationProperties
) : FxTask<Unit, EmbeddedProgressController>(
    restTemplateService,
    applicationProperties,
    createProgressWindow(Stage.getWindows().find { it.isShowing })
) {

    override suspend fun process() {
        progressController.show()
        progressController.setDescription("Проверка данных")
        progressController.updateProgress(0, 0)
        groupService.removeAll()
        loadSchema()
        saveSchemaMD5()
        findActiveConfiguration()
        progressController.setDescription("Настройка завершена")
        progressController.setCloseButtonVisible(true)
    }


    private suspend fun loadSchema() {
        progressController.setDescription("Чтение конфигурации")
        progressController.updateProgress(0, 0)
        val jsons = fileService.readFile(files.schema).trim().split("\n")
        val counter = AtomicInteger()
        val groups: List<Group> = parallelCalculation(jsons) { json ->
            val group: Group = mapper.readValue(json)
            group.releases.forEach { release: Release ->
                release.group = group
                release.details.forEach { details ->
                    details.release = release
                }
            }
            progressController.updateProgress(counter.incrementAndGet(), jsons.size)
            group
        }
        progressController.setDescription("Сохранение данных")
        progressController.updateProgress(1, 1)
        groupService.saveAll(groups)
    }


    private suspend fun saveSchemaMD5() {
        val properties = propertiesRepository.findByKey(PropertyKey.SCHEMA)
            ?: Properties(PropertyKey.SCHEMA)
        val md5 = Arrays.toString(fileService.getFileMD5(files.schema))
        properties.value = md5
        propertiesRepository.save(properties)
    }

    private suspend fun findActiveConfiguration() {
        progressController.setDescription("Поиск активной конфигурации")
        progressController.updateProgress(0, 0)
        val groups = groupService.getAllWithDetails()
        val counter = AtomicInteger()
        val max = groups.flatMap { group -> group.releases }.sumBy { release -> release.details.size }
        groups.forEach { group ->
            progressController.setDescription("Поиск активной конфигурации для ${group.value}")
            parallelCalculation(group.releases) { release ->
                val details = detailsRepository.findAllByRelease(release)
                release.applied = true
                details.forEach { detail ->
                    if (release.applied) {
                        val gameFile = File(files.gameFolder.absolutePath + File.separator + detail.gamePath)
                        val storageFile = File(files.optional.absolutePath + File.separator + detail.storagePath)
                        val gameFileMD5 = fileService.getFileMD5(gameFile)
                        val storageFileMD5 = fileService.getFileMD5(storageFile)
                        if (!Arrays.equals(gameFileMD5, storageFileMD5)) {
                            release.applied = false
                        }
                    }
                    progressController.updateProgress(counter.incrementAndGet(), max)
                }
            }
        }
        progressController.updateProgress(1, 1)
        progressController.setDescription("Сохранение данных")
        groupService.saveAll(groups)
    }
}