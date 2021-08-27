package com.lezenford.mfr.server.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.PreDestroy

@Configuration
class MultithreadingConfiguration {
    private val historyUpdateExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    @Bean
    fun serverGlobalFileLock(): ReentrantReadWriteLock = ReentrantReadWriteLock(true)

    @Bean
    fun historyUpdateExecutor(): ExecutorService = historyUpdateExecutor

    @PreDestroy
    fun close() {
        historyUpdateExecutor.shutdown()
    }
}