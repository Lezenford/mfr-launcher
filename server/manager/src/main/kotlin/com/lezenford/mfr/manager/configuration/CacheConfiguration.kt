package com.lezenford.mfr.manager.configuration

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
                        USER_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(1, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        MODULE_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(30, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        JWT_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterWrite(50, TimeUnit.MINUTES)
                            .ticker(ticker)
                            .build()
                    )
                )
            )
        }
}

const val USER_CACHE = "UserCache"
const val MODULE_CACHE = "ModuleCache"
const val JWT_CACHE = "JwtCache"