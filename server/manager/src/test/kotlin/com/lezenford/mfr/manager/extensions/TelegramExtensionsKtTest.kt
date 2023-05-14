package com.lezenford.mfr.manager.extensions

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.objects.CallbackQuery

internal class TelegramExtensionsKtTest {

    @Test
    fun `convert full min value array`() {
        for (i in 0..8) {
            val source = IntArray(i) { 0 }
            val result = CallbackQuery().apply { data = source.convert() }.convert()
            Assertions.assertThat(source.contentEquals(result)).`as`("content with size = $i").isTrue
        }
    }

    @Test
    fun `convert full max value array`() {
        for (i in 0..8) {
            val source = IntArray(i) { Int.MAX_VALUE }
            val result = CallbackQuery().apply { data = source.convert() }.convert()
            Assertions.assertThat(source.contentEquals(result)).`as`("content with size = $i").isTrue
        }
    }

    @Test
    fun `convert different numbers array`() {
        for (i in 1..8) {
            var value = Int.MIN_VALUE
            fun nextValue(): Int = value + 1234567
            do {
                val source = IntArray(i) { value.also { value = nextValue() } }
                val result = CallbackQuery().apply { data = source.convert() }.convert()
                Assertions.assertThat(source.contentEquals(result)).`as`("content with size = $i").isTrue
            } while (nextValue() > value)
        }
    }
}