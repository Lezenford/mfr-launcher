package com.lezenford.mfr.server.extensions

import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.model.entity.Category
import com.lezenford.mfr.server.model.entity.File
import com.lezenford.mfr.server.model.entity.Item
import com.lezenford.mfr.server.model.entity.Launcher
import ru.fullrest.mfr.common.api.rest.BuildDto
import ru.fullrest.mfr.common.api.rest.Client
import ru.fullrest.mfr.common.api.rest.Content
import java.time.LocalDateTime

fun Build.toBuildDto() = BuildDto(id = id, name = name, lastUpdate = lastUpdateDate)

fun List<Category>.toContent(lastUpdate: LocalDateTime? = null) =
    Content(categories = map { it.toContentCategory(lastUpdate) }.filter { it.items.isNotEmpty() })

fun Category.toContentCategory(lastUpdate: LocalDateTime? = null) =
    Content.Category(
        type = type,
        required = required,
        items = items.map { it.toContentCategoryItem(lastUpdate) }.filter { it.files.isNotEmpty() }
    )

fun Item.toContentCategoryItem(lastUpdate: LocalDateTime? = null) =
    Content.Category.Item(
        name = name,
        files = files.filter { lastUpdate?.isBefore(it.lastChangeDate) ?: true }.map { it.toContentCategoryItemFile() }
    )

fun File.toContentCategoryItemFile() =
    Content.Category.Item.File(
        id = id,
        path = path,
        active = active,
        md5 = md5,
        size = size
    )

fun Launcher.toClient(): Client = Client(
    system = system,
    version = version,
    md5 = md5,
    size = size
)
