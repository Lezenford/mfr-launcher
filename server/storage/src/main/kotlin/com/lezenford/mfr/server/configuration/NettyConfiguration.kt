package com.lezenford.mfr.server.configuration

import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.traffic.AbstractTrafficShapingHandler
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import io.netty.util.concurrent.GlobalEventExecutor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NettyConfiguration {

    @Bean
    fun clients(): ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    @Bean
    fun gameDownloadClients(): ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    @Bean
    fun launcherDownloadClients(): ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    @Bean
    fun channelTrafficHandler(): AbstractTrafficShapingHandler =
        GlobalTrafficShapingHandler(GlobalEventExecutor.INSTANCE, 1000)
}