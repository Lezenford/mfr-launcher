package ru.fullrest.mfr.common.extensions

fun Double.format(digits: Int) = "%.${digits}f".format(this)