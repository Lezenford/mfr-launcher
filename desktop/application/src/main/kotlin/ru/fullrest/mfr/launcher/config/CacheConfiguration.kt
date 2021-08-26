package ru.fullrest.mfr.launcher.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

const val SECTION_CACHE = "section"
const val EXTRA_CACHE = "extra"

@Configuration
class CacheConfiguration {
    @Bean
    fun ticker(): Ticker =
        Ticker.systemTicker();

    @Bean
    fun cacheManager(ticker: Ticker): CacheManager =
        SimpleCacheManager().also {
            it.setCaches(
                listOf(
                    CaffeineCache(
                        SECTION_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(15, TimeUnit.MINUTES)
                            .ticker(ticker)
                            .build()
                    )
                )
            )
        }
}