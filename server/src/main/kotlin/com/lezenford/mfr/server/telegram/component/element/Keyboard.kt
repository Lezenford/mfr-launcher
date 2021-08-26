package com.lezenford.mfr.server.telegram.component.element

import com.fasterxml.jackson.annotation.JsonProperty
import com.lezenford.mfr.server.extensions.Dsl
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Dsl
class Keyboard private constructor(
    @JsonProperty("inline_keyboard")
    private val keys: MutableList<List<InlineKeyboardButton>>
) : InlineKeyboardMarkup(keys) {
    fun line(init: Line.Key.() -> Unit) {
        keys.add(Line { key { init() } })
    }

    fun multipleLine(init: Line.() -> Unit) {
        keys.add(Line { init() })
    }

    fun <T> lines(items: Collection<T>, init: Line.Key.(T) -> Unit) {
        keys.addAll(items.map { Line { key { init(it) } } })
    }

    companion object {
        operator fun invoke(init: Keyboard.() -> Unit): Keyboard =
            Keyboard(mutableListOf()).also { it.init() }
    }

    @Dsl
    class Line private constructor(
        private val keyboardLine: MutableList<Key>
    ) : List<Line.Key> by keyboardLine {

        fun key(init: Key.() -> Unit) {
            keyboardLine.add(Key().apply { init() })
        }

        companion object {
            operator fun invoke(init: Line.() -> Unit): Line =
                Line(mutableListOf()).also { it.init() }
        }

        @Dsl
        class Key : InlineKeyboardButton()
    }
}

