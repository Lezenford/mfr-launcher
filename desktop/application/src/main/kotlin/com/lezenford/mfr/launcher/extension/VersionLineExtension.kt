package com.lezenford.mfr.launcher.extension

/**
 * Линия совместимости — два первых октета номера версии: `1.5.3` → `1.5`. Октеты — числа,
 * поэтому `01.5` и `1.5` — одна линия; так линию выводит сервер, и клиент следует тому же правилу.
 */
fun String.versionLine(): String? = lineOctets()?.let { (major, minor) -> "$major.$minor" }

/**
 * Сравнение линий по октетам. Неразбираемая запись считается младше любой разбираемой.
 */
fun compareVersionLines(left: String, right: String): Int {
    val a = left.lineOctets()
    val b = right.lineOctets()
    return when {
        a != null && b != null -> compareValuesBy(a, b, { it.first }, { it.second })
        a != null -> 1
        b != null -> -1
        else -> left.compareTo(right)
    }
}

private fun String.lineOctets(): Pair<Int, Int>? {
    val parts = trim().split('.')
    if (parts.size < 2) return null
    val major = parts[0].toIntOrNull() ?: return null
    val minor = parts[1].toIntOrNull() ?: return null
    return major to minor
}
