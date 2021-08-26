package ru.fullrest.mfr.launcher.javafx.component

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.model.entity.Extra
import ru.fullrest.mfr.launcher.service.ExtraService
import kotlin.io.path.deleteIfExists

class DownloadExtraButton(
    private val extra: Extra,
    private val applicationProperties: ApplicationProperties,
    private val extraService: ExtraService,
    downloadEvent: DownloadButton.() -> Unit
) : DownloadButton(downloadEvent) {
    init {
        setStatus(if (extra.downloaded) Status.REMOVE else Status.DOWNLOAD)
    }

    override fun removeOption() {
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.JavaFx) { isDisable = true }
            try {
                extra.files.forEach {
                    applicationProperties.gameFolder.resolve(it.path.toPath()).deleteIfExists()
                }
                extra.downloaded = false
                extraService.save(extra)
                setStatus(Status.DOWNLOAD)
            } finally {
                withContext(Dispatchers.JavaFx) { isDisable = false }
            }
        }
    }
}