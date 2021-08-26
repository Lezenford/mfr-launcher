package com.lezenford.mfr.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.exception.OperationException
import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.model.entity.Category
import com.lezenford.mfr.server.model.entity.File
import com.lezenford.mfr.server.model.entity.Item
import com.lezenford.mfr.server.service.model.BuildService
import com.lezenford.mfr.server.service.model.CategoryService
import org.springframework.stereotype.Service
import ru.fullrest.mfr.common.api.ContentType
import ru.fullrest.mfr.common.api.json.CONTENT_FILE_NAME
import ru.fullrest.mfr.common.api.json.Content
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.common.extensions.toPath
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.transaction.Transactional
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readBytes

@Service
class StorageService(
    private val categoryService: CategoryService,
    private val buildService: BuildService,
    private val serverSettingProperties: ServerSettingProperties,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun updateBuild(build: Build) {
        log.info("Create game info operation started for build ${build.name}")
        val repository = Paths.get(serverSettingProperties.buildFolder).resolve(build.branch)
        log.info("Repository path: $repository")
        val updateDate = LocalDateTime.now(ZoneOffset.UTC)
        log.info("Update date is $updateDate")
        val content = repository.resolve(CONTENT_FILE_NAME).takeIf { it.exists() }?.readBytes()
            ?.let { objectMapper.readValue<Content>(it) }
            ?: throw OperationException("File $CONTENT_FILE_NAME not found in repository $repository")
        log.info("Content successfully read")
        val categories = updateCategories(build, updateDate, content)
        log.info("Categories successfully updated")
        build.lastUpdateDate = updateDate
        categories.flatMap { category -> category.items.flatMap { it.files } }.filter { it.active }.forEach {
            val path = repository.resolve(it.path.toPath())
            val md5 = path.md5()
            if (it.md5.contentEquals(md5).not()) {
                it.md5 = md5
                it.size = path.fileSize()
                it.lastChangeDate = updateDate
                log.debug("Update MD5 for file ${it.path}")
            }
        }
        log.info("Files md5 successfully updated")
        categoryService.saveAll(categories)
        log.info("Categories successfully saved")
        buildService.save(build)
        log.info("Build successfully saved")
        log.info("Create game info operation successfully finished")
    }

    private fun updateCategories(build: Build, updateDate: LocalDateTime, content: Content): List<Category> {
        val incomeCategories = content.categories
        val savedCategories = categoryService.findAllByBuildId(build.id).toMutableList()

        ContentType.values().forEach { categoryType ->
            val buildCategory = incomeCategories.find { it.type == categoryType }
                ?: throw OperationException("Build structure doesn't exist category $categoryType")
            val savedCategory = savedCategories.find { it.type == categoryType }
                ?: Category(
                    type = categoryType,
                    required = buildCategory.required,
                    build = build
                ).also {
                    savedCategories.add(it)
                    log.info("Create new category: {type: ${it.type}, required: ${it.required}}")
                }
            val savedCategoryItems: Map<String, Item> = savedCategory.items.associateBy { it.name }
            val incomeCategoryItems: Map<String, Content.Category.Item> = buildCategory.items.associateBy { it.name }
            buildCategory.items.forEach { item ->
                val savedItem = savedCategoryItems[item.name]
                    ?: Item(
                        name = item.name,
                        category = savedCategory
                    ).also {
                        savedCategory.items.add(it)
                        log.info("Create new item: {name: ${it.name}, categoryType: ${savedCategory.type}}")
                    }
                val savedItemFiles: Map<String, File> = savedItem.files.associateBy { it.path }
                val incomeItemFiles: Map<String, Content.Category.Item.File> = item.files.associateBy { it.path }
                item.files.forEach { file ->
                    savedItemFiles[file.path]
                        ?: File(
                            path = file.path,
                            lastChangeDate = updateDate,
                            item = savedItem
                        ).also {
                            savedItem.files.add(it)
                            log.debug("Create new file: {path: ${it.path}, itemName: ${savedItem.name}, categoryType: ${savedCategory.type}}")
                        }
                }
                //Mark files in existent items
                savedItemFiles.forEach {
                    incomeItemFiles[it.key] ?: it.value.markAsDeleted(it.key, updateDate)
                }
            }
            //Mark files in non-existent items
            savedCategoryItems.forEach {
                incomeCategoryItems[it.key] ?: it.value.files.forEach { file ->
                    file.markAsDeleted(it.key, updateDate)
                }
            }
        }
        return savedCategories;
    }

    private fun File.markAsDeleted(itemName: String, updateDate: LocalDateTime) {
        active = false
        lastChangeDate = updateDate
        md5 = ByteArray(0)
        log.info("Mark file with path: $path from item $itemName as deleted")
    }

    companion object {
        private val log by Logger()
    }
}