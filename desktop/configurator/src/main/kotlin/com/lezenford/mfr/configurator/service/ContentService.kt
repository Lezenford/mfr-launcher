package com.lezenford.mfr.configurator.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.common.extensions.takeIfInstance
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.common.protocol.file.CONTENT_FILE_NAME
import com.lezenford.mfr.common.protocol.file.Content
import com.lezenford.mfr.common.protocol.file.SCHEMA_FILE_NAME
import com.lezenford.mfr.common.protocol.file.Schema
import com.lezenford.mfr.configurator.content.AdditionalContent
import com.lezenford.mfr.configurator.content.FileTree
import com.lezenford.mfr.configurator.content.GameFile
import com.lezenford.mfr.configurator.content.MainContent
import com.lezenford.mfr.configurator.content.SwitchableContent
import com.lezenford.mfr.javafx.extensions.runFx
import javafx.scene.control.Alert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

@Service
class ContentService(
    private val mainContent: MainContent,
    private val additionalContents: MutableSet<AdditionalContent>,
    private val switchableContents: MutableSet<SwitchableContent>,
    private val objectMapper: ObjectMapper,
    private val rootFolderReference: AtomicReference<Path>,
    private val fileTreeService: FileTreeService,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScope = Dispatchers.IO.limitedParallelism(Runtime.getRuntime().availableProcessors())
    private val schemaFile: Path
        get() = rootFolderReference.get()?.resolve(SCHEMA_FILE_NAME)?.also { if (!it.exists()) it.createFile() }
            ?: throw IllegalArgumentException("Root folder didn't init")
    private val contentFile: Path
        get() = rootFolderReference.get()?.resolve(CONTENT_FILE_NAME)?.also { if (!it.exists()) it.createFile() }
            ?: throw IllegalArgumentException("Root folder didn't init")

    fun load() {
        val contentData = contentFile.readBytes().takeIf { it.isNotEmpty() }
            ?.let { objectMapper.readValue<Content>(it) }
            ?: return
        val schemaData = schemaFile.readBytes().takeIf { it.isNotEmpty() }
            ?.let { objectMapper.readValue<Schema>(it) }
            ?: return
        val tree = fileTreeService.tree
        contentData.categories.first { it.type == ContentType.MAIN }.items.first().files.forEach {
            it.convert(tree, mainContent)
        }
        contentData.categories.first { it.type == ContentType.EXTRA }.items.forEach { category ->
            val additionalContent = AdditionalContent(category.name)
            additionalContents.add(additionalContent)
            category.files.forEach { it.convert(tree, additionalContent) }
        }
        schemaData.packages.forEach { category ->
            val switchableContent = SwitchableContent(category.name)
            switchableContents.add(switchableContent)
            category.options.forEach { option ->
                val optionContent = SwitchableContent.Option(
                    AdditionalContent(
                        name = option.name,
                        description = option.description,
                        image = option.image?.let { imageRelativePath ->
                            tree.find(imageRelativePath.toPath())?.takeIfInstance<GameFile>()
                        })
                )
                switchableContent.options.add(optionContent)
                option.items.forEach { item ->
                    tree.find(item.storagePath.toPath())?.also { file ->
                        file.takeIfInstance<GameFile>()?.also { gameFile ->
                            optionContent.files.add(gameFile)
                            optionContent.alternativePaths[gameFile.id] = item.gamePath.toPath()
                        } ?: log.warn("File ${item.storagePath} is directory and can't be as a part of content")
                    } ?: log.warn("File ${item.storagePath} not found for content ${category.name} / ${option.name}")
                }
            }
        }
    }

    suspend fun saveSchema() {
        val content = Schema(
            packages = switchableContents.map { `package` ->
                Schema.Package(
                    name = `package`.name.value,
                    options = `package`.options.map { option ->
                        CoroutineScope(Dispatchers.IO).async {
                            Schema.Package.Option(
                                name = option.name.value,
                                description = option.description,
                                image = option.image?.relativePath?.toString(),
                                items = option.files.map {
                                    CoroutineScope(coroutineScope).async {
                                        Schema.Package.Option.Item(
                                            storagePath = it.relativePath.toString(),
                                            gamePath = option.alternativePath(it).toString(),
                                            md5 = it.absolutePath.md5()
                                        )
                                    }
                                }.map { it.await() })
                        }
                    }.map { it.await() })
            },
            extra = additionalContents.map { content ->
                CoroutineScope(Dispatchers.IO).async {
                    Schema.Extra(
                        name = content.name.value,
                        items = content.files.map { file ->
                            CoroutineScope(coroutineScope).async {
                                Schema.Extra.Item(
                                    path = file.toString(), md5 = file.absolutePath.md5()
                                )
                            }
                        }.map { it.await() })
                }
            }.map { it.await() }
        )
        schemaFile.writeText(objectMapper.writeValueAsString(content))
    }

    fun validate(): Boolean {
        val tree = fileTreeService.tree

        val mainContentErrors = mainContent.files.filterNot { tree[it.id]?.hidden == false }.map { it.relativePath }
        if (mainContentErrors.isNotEmpty()) {
            runFx {
                Alert(
                    Alert.AlertType.ERROR,
                    "Main content contains incorrect files and operation will be dropped: $mainContentErrors"
                ).showAndWait()
            }
            return false
        }
        val additionalContentErrors =
            additionalContents.asSequence().flatMap { it.files }.filterNot { tree[it.id]?.hidden == false }
                .mapTo(mutableListOf()) { it.relativePath }
        if (additionalContentErrors.isNotEmpty()) {
            runFx {
                Alert(
                    Alert.AlertType.ERROR,
                    "Additional content contains incorrect files and operation will be dropped: $additionalContentErrors"
                ).showAndWait()
            }
            return false
        }
        val switchableContentErrors =
            switchableContents.asSequence().flatMap { it.files }.filterNot { tree[it.id]?.hidden == false }
                .mapTo(mutableListOf()) { it.relativePath }
        if (switchableContentErrors.isNotEmpty()) {
            runFx {
                Alert(
                    Alert.AlertType.ERROR,
                    "Optional content contains incorrect files and operation will be dropped: $switchableContentErrors"
                ).showAndWait()
            }
            return false
        }
        return true
    }

    fun saveContent() {
        if (mainContent.files.none { it.relativePath.toString() == SCHEMA_FILE_NAME }) {
            mainContent.files.add(
                GameFile(rootFolderReference.get(), rootFolderReference.get().resolve(SCHEMA_FILE_NAME))
            )
        }
        val content = Content(
            categories = listOf(
                Content.Category(
                    type = ContentType.MAIN,
                    required = true,
                    items = listOf(
                        Content.Category.Item(name = mainContent.name.value, files = mainContent.files.map {
                            Content.Category.Item.File(path = it.relativePath.toString())
                        })
                    )
                ),
                Content.Category(
                    type = ContentType.EXTRA,
                    required = false,
                    items = additionalContents.map { category ->
                        Content.Category.Item(name = category.name.value, files = category.files.map {
                            Content.Category.Item.File(path = it.relativePath.toString())
                        })
                    }),

                Content.Category(
                    type = ContentType.OPTIONAL,
                    required = false,
                    items = switchableContents.map { category ->
                        Content.Category.Item(name = category.name.value, files = category.files.map {
                            Content.Category.Item.File(path = it.relativePath.toString())
                        })
                    }
                ),
            )
        )
        contentFile.writeText(objectMapper.writeValueAsString(content))
    }

    private fun Content.Category.Item.File.convert(
        tree: FileTree,
        content: com.lezenford.mfr.configurator.content.Content
    ) {
        tree.find(path.toPath())?.also { file ->
            if (file is GameFile) {
                content.files.add(file)
            } else {
                log.warn("File $path is directory and can't be as a part of content")
            }
        } ?: kotlin.run {
            if (path != SCHEMA_FILE_NAME) log.warn("File $path not found for content ${content.name.value}")
        }
    }

    companion object {
        private val log by Logger()
    }
}