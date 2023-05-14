package com.lezenford.mfr.server.extensions

import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.server.model.entity.*
import java.time.LocalDateTime

fun Build.toBuildDto() = BuildDto(id = id, name = name, lastUpdate = lastUpdateDate, default = default)

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
