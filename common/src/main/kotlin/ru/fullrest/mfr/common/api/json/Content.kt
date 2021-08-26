package ru.fullrest.mfr.common.api.json

import com.fasterxml.jackson.annotation.JsonAlias
import ru.fullrest.mfr.common.api.ContentType

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
            //        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
            data class File(
                val path: String
            )
        }
    }
}