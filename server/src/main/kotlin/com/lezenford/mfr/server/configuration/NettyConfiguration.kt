package com.lezenford.mfr.server.configuration

import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NettyConfiguration {

    @Bean
    fun channels(): ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
}