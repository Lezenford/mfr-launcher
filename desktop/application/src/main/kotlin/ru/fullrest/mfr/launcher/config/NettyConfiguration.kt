package ru.fullrest.mfr.launcher.config

import io.netty.channel.nio.NioEventLoopGroup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.PreDestroy

@Configuration
class NettyConfiguration {
    private val workGroup = NioEventLoopGroup(1)

    @Bean
    fun nettyClientWorkGroup() = workGroup

    @PreDestroy
    fun preDestroy() {
        workGroup.shutdownGracefully()
    }
}