package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.launcher.exception.ServerConnectionException
import com.lezenford.mfr.launcher.exception.TaskExecuteException
import com.lezenford.mfr.launcher.service.State
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext

abstract class Task<K, T> : CoroutineScope {
    final override val coroutineContext: CoroutineContext = Dispatchers.IO
    private val mutableProgressState: MutableStateFlow<Int> = MutableStateFlow(0)
    private val mutableDescriptionState: MutableStateFlow<String> = MutableStateFlow("")
    private val mutableState: MutableStateFlow<Status> = MutableStateFlow(Status.NEW)

    val status: StateFlow<Status> = mutableState.asStateFlow()
    val progress: StateFlow<Int> = mutableProgressState.asStateFlow()
    val description: StateFlow<String> = mutableDescriptionState.asStateFlow()

    val isDone: Boolean
        get() = status.value == Status.FINISHED

    suspend fun execute(params: K): T {
        if (State.serverConnection.value.not()) {
            log.info("Task can not be executed without server connection")
            throw ServerConnectionException()
        }
        return try {
            mutableState.emit(Status.IN_PROGRESS)
            withContext(coroutineContext) {
                log.info("Task ${this@Task::class.simpleName} starting")
                val result = action(params)
                log.info("Task ${this@Task::class.simpleName} successfully finished")
                return@withContext result
            }
        } catch (e: Exception) {
            throw TaskExecuteException(e)
        } finally {
            mutableState.emit(Status.FINISHED)
        }
    }


    protected abstract suspend fun action(params: K): T

    protected suspend fun <E> joinSubtask(task: Task<Unit, E>): E = joinSubtask(task, Unit)

    protected suspend fun <R, E> joinSubtask(task: Task<R, E>, params: R): E {
        return coroutineScope {
            val descriptionJob = launch { task.description.collect { updateDescription(it) } }
            val progressJob = launch { task.progress.collect { updateProgress(it) } }

            val result = task.execute(params)

            descriptionJob.cancel()
            progressJob.cancel()

            result
        }
    }

    protected suspend fun updateProgress(currentValue: Long, maxValue: Long) {
        updateProgress(calculateProgress(currentValue, maxValue))
    }

    protected suspend fun updateProgress(value: Int) {
        mutableProgressState.emit(value)
    }

    protected suspend fun updateDescription(description: String) {
        mutableDescriptionState.emit(description)
    }

    private fun calculateProgress(current: Long, max: Long): Int {
        return if (max == 0L) {
            0
        } else {
            (current * 100L / max).toInt()
        }
    }

    enum class Status {
        NEW, IN_PROGRESS, FINISHED
    }

    companion object {
        private val log by Logger()
    }
}