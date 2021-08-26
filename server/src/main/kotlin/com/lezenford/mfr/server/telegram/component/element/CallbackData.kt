package com.lezenford.mfr.server.telegram.component.element

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.lezenford.mfr.server.telegram.component.CallbackModule

data class CallbackData(
    @JsonProperty(value = "m")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val module: CallbackModule.Type,

    @JsonProperty(value = "e")
    val event: Int,

    @JsonProperty(value = "d")
    val details: Map<String, String> = emptyMap()
) {

    fun convert(): String {
        return StringBuilder().append(module.ordinal).append(":").append(event).also { builder ->
            details.forEach { builder.append(":").append(it.key).append(":").append(it.value) }
        }.toString()
    }

    companion object {
        operator fun invoke(text: String): CallbackData {
            return text.split(":").chunked(2).filter { it.size == 2 }.takeIf { it.isNotEmpty() }?.let { list ->
                CallbackData(
                    module = CallbackModule.Type.values()[list[0][0].toInt()],
                    event = list[0][1].toInt(),
                    details = list.drop(1).associate { it[0] to it[1] }
                )
            } ?: throw IllegalArgumentException("Incorrect callback data $text")
        }
    }
}