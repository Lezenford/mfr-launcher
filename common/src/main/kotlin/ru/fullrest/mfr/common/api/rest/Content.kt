package ru.fullrest.mfr.common.api.rest

import ru.fullrest.mfr.common.api.ContentType

data class Content(
    val categories: List<Category>
) {
    data class Category(
        val type: ContentType,
        val required: Boolean,
        val items: List<Item>
    ) {
        data class Item(
            val name: String,
            val files: List<File>
        ) {
            data class File(
                val id: Int,
                val path: String,
                val active: Boolean,
                val md5: ByteArray,
                val size: Long
            )
        }
    }
}