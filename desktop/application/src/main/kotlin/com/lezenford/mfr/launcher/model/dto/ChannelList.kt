package com.lezenford.mfr.launcher.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Перечень линий совместимости, доступных этому клиенту (`/v2/game/channels`).
 * Написано в форме сгенерированных моделей: генератора в сборке нет.
 */
@Serializable
data class ChannelList(

    @SerialName(value = "channels")
    val channels: List<Channel>

)

@Serializable
data class Channel(

    @SerialName(value = "id")
    val id: String

)
