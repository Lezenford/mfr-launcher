package com.lezenford.mfr.common.extensions

fun Boolean.ifTrue(action: () -> Unit): Boolean {
    if (this) action()
    return this
}

fun Boolean.ifFalse(action: () -> Unit): Boolean {
    if (this.not()) action()
    return this
}