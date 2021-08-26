package ru.fullrest.mfr.launcher.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.ContentType
import ru.fullrest.mfr.common.api.rest.Content
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.service.ExtraService
import ru.fullrest.mfr.launcher.service.PropertiesService
import ru.fullrest.mfr.launcher.service.RestTemplateService
import ru.fullrest.mfr.launcher.service.SectionService
import java.time.LocalDateTime

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class CheckGameUpdateTask(
    private val restTemplateService: RestTemplateService,
    private val propertiesService: PropertiesService,
    private val sectionService: SectionService,
    private val extraService: ExtraService,
    private val objectMapper: ObjectMapper
) {

    suspend fun execute(): List<Content.Category.Item.File> {
        return propertiesService.findByKey(Properties.Key.LAST_UPDATE_DATE)?.value?.let {
            objectMapper.readValue<LocalDateTime>(it)
        }?.let { lastUpdateDate ->
            val content = restTemplateService.content(lastUpdateDate)
            val savedSections = sectionService.findAllWithDetails()
            val savedExtras = extraService.findAll()

            val newMain =
                content.categories.find { it.type == ContentType.MAIN }?.items?.flatMap { it.files } ?: emptyList()

            val newSections = savedSections.filter { it.downloaded }.mapNotNull { section ->
                content.categories.find { it.type == ContentType.OPTIONAL }?.let { `package` ->
                    `package`.items.find { it.name == section.name }?.files
                }
            }.flatten()

            val newExtras = savedExtras.filter { it.downloaded }.mapNotNull { extra ->
                content.categories.find { it.type == ContentType.EXTRA }?.let { `package` ->
                    `package`.items.find { it.name == extra.name }?.files
                }
            }.flatten()

            newMain + newSections + newExtras
        } ?: emptyList()
    }
}