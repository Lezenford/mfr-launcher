package com.lezenford.mfr.launcher.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun <T> Flow<T>.listener(context: CoroutineContext = Dispatchers.IO, action: suspend (T) -> Unit) {
    CoroutineScope(context).launch { collect { action(it) } }
}