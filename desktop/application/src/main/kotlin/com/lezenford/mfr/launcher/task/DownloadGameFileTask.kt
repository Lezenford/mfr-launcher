package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.extension.toTraffic
import com.lezenford.mfr.launcher.netty.FileData
import com.lezenford.mfr.launcher.service.provider.NettyProvider
import io.netty.handler.traffic.AbstractTrafficShapingHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import kotlin.io.path.exists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DownloadGameFileTask(
    private val nettyProvider: NettyProvider,
    private val nettyProgressFlow: MutableSharedFlow<Long>,
    private val trafficShaping: AbstractTrafficShapingHandler,
    private val properties: ApplicationProperties
) : Task<List<Content.Category.Item.File>, Unit>() {

    override suspend fun action(params: List<Content.Category.Item.File>) {
        updateDescription("Подготовка к скачиванию")

        val totalSize = params.sumOf { it.size }
        var downloaded = 0L

        val target = params.map {
            FileData(
                id = it.id,
                size = it.size,
                path = properties.gameFolder.resolve(it.path),
                md5 = it.md5
            )
        }.partition { it.path.exists() }.let { (exists, nonExists) ->
            updateDescription("Анализ существующих файлов")
            val notEquals = exists.filterNot { file ->
                file.path.md5().contentEquals(file.md5).also {
                    if (it) {
                        downloaded += file.size
                        updateProgress(downloaded, totalSize)
                    }
                }
            }
            nonExists + notEquals
        }

        val descriptionJob = launch {
            while (true) {
                updateDescription("Скорость скачивания: ${trafficShaping.trafficCounter().lastReadBytes().toTraffic()}")
                delay(1000)
            }
        }
        val progressJob = launch {
            nettyProgressFlow.collect {
                downloaded += it
                updateProgress(downloaded, totalSize)
            }
        }

        nettyProvider.downloadGameFiles(target)
        progressJob.cancel()
        descriptionJob.cancel()
    }

    companion object {
        private val log by Logger()
    }
}