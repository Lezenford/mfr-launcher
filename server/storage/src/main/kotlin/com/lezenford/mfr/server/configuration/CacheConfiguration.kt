package com.lezenford.mfr.server.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@EnableCaching
@Configuration
class CacheConfiguration {

    @Bean
    fun ticker(): Ticker = Ticker.systemTicker()

    @Bean
    fun cacheManager(ticker: Ticker): CacheManager =
        SimpleCacheManager().also {
            it.setCaches(
                listOf(
                    CaffeineCache(
                        CATEGORY_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(7, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        FILE_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(7, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        LAUNCHER_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(7, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        HISTORY_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterWrite(15, TimeUnit.MINUTES)
                            .ticker(ticker)
                            .build()
                    )
                )
            )
        }

    companion object {
        const val CATEGORY_CACHE = "CategoryCache"
        const val FILE_CACHE = "FileCache"
        const val LAUNCHER_CACHE = "LauncherCache"
        const val HISTORY_CACHE = "HistoryCache"
    }
}