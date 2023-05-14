package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.netty.RequestGameFilesMessage
import com.lezenford.mfr.server.annotation.Available
import com.lezenford.mfr.server.configuration.History
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.service.MaintenanceService
import com.lezenford.mfr.server.service.model.FileService
import com.lezenford.mfr.server.service.model.OverviewService
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.group.ChannelGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameDownloadFileHandler(
    gameDownloadClients: ChannelGroup,
    private val fileService: FileService,
    private val overviewService: OverviewService,
    private val serverSettingProperties: ServerSettingProperties
) : DownloadFileHandler<RequestGameFilesMessage>(gameDownloadClients) {

    @Available(MaintenanceService.Type.GAME)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        super.channelRead(ctx, msg)
    }

    override fun RequestGameFilesMessage.addToQueue() {
        fileQueue.addAll(
            fileService.findAllByIds(files).filter { it.active }.also { fileList ->
                CoroutineScope(Dispatchers.History).launch {
                    fileList.map { it.item }.distinct().also { items ->
                        kotlin.runCatching { overviewService.updateHistory(items, clientId.toString()) }
                            .onFailure { e ->
                                log.error(
                                    "Update history throw an exception for client: $clientId and items: ${
                                        items.map { it.id }.joinToString(", ")
                                    }", e
                                )
                            }
                    }
                }
            }.map {
                FileState(
                    id = it.id,
                    path = Paths.get(serverSettingProperties.build.local).resolve(it.item.category.build.branch)
                        .resolve(it.path.toPath()).toFile()
                )
            }
        )
    }

    companion object {
        private val log by Logger()
    }
}