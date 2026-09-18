package com.lezenford.mfr.launcher.extension

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionLineExtensionTest {

    @Test
    fun `линия — два первых октета номера версии`() {
        assertEquals("1.5", "1.5.3".versionLine())
        assertEquals("2.0", "2.0".versionLine())
    }

    @Test
    fun `октеты — числа, ведущие нули не образуют другой линии`() {
        assertEquals("1.5", "01.5.0".versionLine())
    }

    @Test
    fun `номер без двух числовых октетов линии не имеет`() {
        assertNull("beta".versionLine())
        assertNull("7".versionLine())
        assertNull("1.x.2".versionLine())
    }

    @Test
    fun `линии сравниваются по октетам, а не как строки`() {
        assertTrue(compareVersionLines("2.0", "1.10") > 0)
        assertTrue(compareVersionLines("1.10", "1.9") > 0)
        assertEquals(0, compareVersionLines("01.5", "1.5"))
        assertTrue(compareVersionLines("garbage", "1.0") < 0)
    }
}
