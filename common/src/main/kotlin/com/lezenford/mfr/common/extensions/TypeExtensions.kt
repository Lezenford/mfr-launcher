package com.lezenford.mfr.common.extensions

inline fun <reified T> Any.takeIfInstance(): T? {
    return if (this is T) this else null
}