package com.lezenford.mfr.server.configuration

import com.lezenford.mfr.common.extensions.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

private val historyUpdateExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

@Suppress("unused")
val Dispatchers.History: CoroutineDispatcher by lazy { historyUpdateExecutor.asCoroutineDispatcher() }

@Configuration
class CoroutineDispatcherConfiguration {

    @PreDestroy
    fun close() {
        log.info("historyUpdateExecutor shutdown")
        historyUpdateExecutor.shutdown()
    }

    companion object {
        private val log by Logger()
    }
}