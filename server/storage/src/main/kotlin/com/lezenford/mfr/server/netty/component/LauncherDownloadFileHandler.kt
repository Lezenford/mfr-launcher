package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.netty.RequestLauncherFilesMessage
import com.lezenford.mfr.server.annotation.Available
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.service.MaintenanceService
import com.lezenford.mfr.server.service.model.LauncherService
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.group.ChannelGroup
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class LauncherDownloadFileHandler(
    launcherDownloadClients: ChannelGroup,
    private val launcherService: LauncherService,
    private val serverSettingProperties: ServerSettingProperties
) : DownloadFileHandler<RequestLauncherFilesMessage>(launcherDownloadClients) {

    @Available(MaintenanceService.Type.LAUNCHER)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        super.channelRead(ctx, msg)
    }

    override fun RequestLauncherFilesMessage.addToQueue() {
        fileQueue.addAll(
            launcherService.findAll().find { it.system == systemType }?.let { launcher ->
                serverSettingProperties.launcherFolder.toPath().resolve(systemType.toString())
                    .resolve(launcher.fileName)
                    .toFile().let { listOf(FileState(path = it)) }
            } ?: emptyList()
        )
    }
}