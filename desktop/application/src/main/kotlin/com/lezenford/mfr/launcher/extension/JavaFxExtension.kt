package com.lezenford.mfr.launcher.extension

import com.lezenford.mfr.launcher.task.Task
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import com.lezenford.mfr.javafx.component.ProgressBar

suspend fun <K, T> ProgressBar.bind(task: Task<K, T>, action: suspend (Task<K, T>) -> T): T {
    val progressJob = launch { task.progress.takeWhile { task.isDone.not() }.collect { updateProgress(it) } }
    val descriptionJob = launch { task.description.takeWhile { task.isDone.not() }.collect { updateDescription(it) } }
    val result = action(task)
    progressJob.cancel()
    descriptionJob.cancel()
    return result
}