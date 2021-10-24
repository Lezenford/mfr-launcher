package com.lezenford.mfr.server.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.fullrest.mfr.common.extensions.Logger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

@Configuration
class MultithreadingConfiguration {
    private val historyUpdateExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val uploadFileExecutor: ExecutorService = Executors.newCachedThreadPool()

    @Bean
    fun historyUpdateExecutor(): ExecutorService = historyUpdateExecutor

    @Bean
    fun uploadFileExecutor(): ExecutorService = uploadFileExecutor

    @PreDestroy
    fun close() {
        log.info("historyUpdateExecutor shutdown")
        historyUpdateExecutor.shutdown()
        log.info("uploadFileExecutor shutdown")
        uploadFileExecutor.shutdown()
    }

    companion object {
        private val log by Logger()
    }
}