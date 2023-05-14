package com.lezenford.mfr.launcher.exception.handler

import com.lezenford.mfr.common.extensions.Logger


abstract class AbstractExceptionHandler(

) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread?, e: Throwable) {
        val exception = clearException(e)

        log.error("handle exception: ${e.message}", e)

        showError(exception)
    }

    protected abstract fun clearException(e: Throwable): Throwable

    protected abstract fun showError(e: Throwable)

    protected companion object {
        private val log by Logger()
        const val DEFAULT_DESCRIPTION = "Произошла ошибка"
    }
}