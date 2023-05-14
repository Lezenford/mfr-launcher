package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.launcher.extension.toTraffic
import com.lezenford.mfr.launcher.service.provider.NettyProvider
import io.netty.handler.traffic.AbstractTrafficShapingHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DownloadLauncherFileTask(
    private val nettyProvider: NettyProvider,
    private val nettyProgressFlow: MutableSharedFlow<Long>,
    private val trafficShaping: AbstractTrafficShapingHandler
) : Task<Client, Path>() {
    override suspend fun action(params: Client): Path {
        val tempFile = Files.createTempFile("mfr_launcher", "")
        val descriptionJob = launch {
            while (true) {
                updateDescription(
                    "Скорость скачивания: ${trafficShaping.trafficCounter().lastReadBytes().toTraffic()}"
                )
                delay(1000)
            }
        }
        val progressJob = launch {
            var downloaded = 0L
            nettyProgressFlow.collect {
                downloaded += it
                updateProgress(downloaded, params.size)
            }
        }
        nettyProvider.downloadLauncher(tempFile, params)
        progressJob.cancel()
        descriptionJob.cancel()
        return tempFile
    }
}