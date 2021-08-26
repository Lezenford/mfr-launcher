package ru.fullrest.mfr.launcher.component

import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.launcher.event.ShowStageRequestEvent

@Component
class ControllerListener(
    private val context: ApplicationContext
) {

    /**
     * Слушает запросы на отображение окна на экране
     */
    @EventListener
    fun showController(event: ShowStageRequestEvent<FxController>) {
        context.getBean(event.target.java).also {
            event.init(it)
        }.show()
    }
}