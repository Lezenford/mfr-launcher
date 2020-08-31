package ru.fullrest.mfr.plugins_configuration_utility.javafx.task

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxTask
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository
import ru.fullrest.mfr.plugins_configuration_utility.service.FileService
import ru.fullrest.mfr.plugins_configuration_utility.util.parallelCalculation
import java.util.concurrent.atomic.AtomicInteger

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class PluginTask(
    private val releaseRepository: ReleaseRepository,
    private val detailsRepository: DetailsRepository,
    private val fileService: FileService
) : FxTask<Unit>() {
    lateinit var releases: List<Release>
    override suspend fun process() {
        releases.forEach { release ->
            releaseRepository.findAllByGroup(release.group!!)
                .filter { it.applied }
                .onEach { disablePlugin(it) }
            enablePlugin(release)
        }
        progressController.setDescription("Изменения установлены")
        progressController.setCloseButtonVisible(true)
    }

    private suspend fun enablePlugin(release: Release) {
        progressController.setDescription(String.format("Подключение %s", release.value))
        progressController.updateProgress(0, 0)
        val details = detailsRepository.findAllByRelease(release)
        val counter = AtomicInteger()
        val max = details.size
        parallelCalculation(details) {
            fileService.copyToGameDirectory(it)
            progressController.updateProgress(counter.incrementAndGet(), max)
        }
        progressController.updateProgress(1, 1)
        progressController.setDescription("Сохранение изменений")
        release.applied = true
        releaseRepository.save(release)
    }

    private suspend fun disablePlugin(release: Release) {
        progressController.setDescription(String.format("Отключение %s", release.value))
        progressController.updateProgress(0, 0)
        val details = detailsRepository.findAllByRelease(release)
        val max = details.size
        val counter = AtomicInteger()
        parallelCalculation(details) {
            fileService.removeFromGameDirectory(it)
            progressController.updateProgress(counter.incrementAndGet(), max)
        }
        progressController.updateProgress(1, 1)
        progressController.setDescription("Сохранение изменений")
        release.applied = false
        releaseRepository.save(release)
    }
}