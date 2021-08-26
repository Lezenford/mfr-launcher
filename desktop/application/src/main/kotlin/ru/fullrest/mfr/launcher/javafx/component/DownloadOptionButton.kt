package ru.fullrest.mfr.launcher.javafx.component

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.model.entity.Section
import ru.fullrest.mfr.launcher.service.SectionService
import kotlin.io.path.deleteIfExists

class DownloadOptionButton(
    private val section: Section,
    private val applicationProperties: ApplicationProperties,
    private val sectionService: SectionService,
    downloadEvent: DownloadButton.() -> Unit
) : DownloadButton(downloadEvent) {
    init {
        setStatus(if (section.downloaded) Status.REMOVE else Status.DOWNLOAD)
    }

    override fun removeOption() {
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.JavaFx) { isDisable = true }
            try {
                section.options.flatMap { it.items }.forEach {
                    applicationProperties.gameFolder.resolve(it.storagePath.toPath()).deleteIfExists()
                }
                section.downloaded = false
                sectionService.save(section)
                setStatus(Status.DOWNLOAD)
            } finally {
                withContext(Dispatchers.JavaFx) { isDisable = false }
            }
        }
    }
}