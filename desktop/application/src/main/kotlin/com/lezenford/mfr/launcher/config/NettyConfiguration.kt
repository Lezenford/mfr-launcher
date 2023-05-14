package com.lezenford.mfr.launcher.config

import com.lezenford.mfr.launcher.service.State
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.traffic.AbstractTrafficShapingHandler
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import io.netty.util.concurrent.GlobalEventExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.PreDestroy

@Configuration
class NettyConfiguration {
    private val workGroup = NioEventLoopGroup(1)

    @Bean
    fun nettyClientWorkGroup() = workGroup

    @Bean
    fun nettyProgressFlow(): MutableSharedFlow<Long> = MutableSharedFlow()

    @Bean
    fun channelTrafficShapingHandler(): AbstractTrafficShapingHandler {
        return GlobalTrafficShapingHandler(GlobalEventExecutor.INSTANCE, 1000).also { handler ->
            CoroutineScope(Dispatchers.IO).launch {
                State.speedLimit.collect { handler.readLimit = (it * 1024).toLong() }
            }
        }
    }

    @PreDestroy
    fun preDestroy() {
        workGroup.shutdownGracefully()
    }
}