package com.lezenford.mfr.manager.configuration

import okhttp3.mockwebserver.MockWebServer
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Profile("test")
@Configuration
@ComponentScan("com.lezenford.mfr.manager")
class BaseTestConfiguration : BeanPostProcessor {

    @get:Bean
    val mockWebServer: MockWebServer = MockWebServer().also { it.start() }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        return when (bean) {
            is WebClient -> when (beanName) {
                "storageServiceWebClient" -> bean.mutate().baseUrl("http://localhost:${mockWebServer.port}").build()
                "telegramWebClient" -> bean.mutate().baseUrl("http://localhost:${mockWebServer.port}/bot/").build()
                else -> super.postProcessAfterInitialization(bean, beanName)
            }

            else -> super.postProcessAfterInitialization(bean, beanName)
        }
    }
}