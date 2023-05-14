package com.lezenford.mfr.common.protocol.file

import com.fasterxml.jackson.annotation.JsonAlias
import com.lezenford.mfr.common.protocol.enums.ContentType

data class Content(
    val categories: List<Category>
) {
    data class Category(
        @JsonAlias("name", "type")
        val type: ContentType,
        val required: Boolean,
        val items: List<Item>
    ) {
        data class Item(
            val name: String,
            val files: List<File>
        ) {
            data class File(
                val path: String
            )
        }
    }
}