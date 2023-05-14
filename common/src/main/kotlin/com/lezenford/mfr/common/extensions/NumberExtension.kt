package com.lezenford.mfr.common.extensions

fun Double.format(digits: Int) = "%.${digits}f".format(this)