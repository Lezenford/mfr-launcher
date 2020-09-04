package ru.fullrest.mfr.server.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String PROPERTY_CACHE = "PropertyCache";
    public static final String USER_CACHE = "UserCache";
    public static final String UPDATE_CACHE = "UpdateCache";
    public static final String STATISTICS_CACHE = "StatisticsCache";

    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    @Bean
    public CacheManager cacheManager(Ticker ticker) {
        ArrayList<Cache> caches = new ArrayList<>();
        caches.add( new CaffeineCache(
                PROPERTY_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .ticker(ticker)
                        .build()
        ));
        caches.add( new CaffeineCache(
                USER_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .ticker(ticker)
                        .build()
        ));
        caches.add( new CaffeineCache(
                UPDATE_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .ticker(ticker)
                        .build()
        ));
        caches.add( new CaffeineCache(
                STATISTICS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .ticker(ticker)
                        .build()
        ));

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
