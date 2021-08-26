package ru.fullrest.mfr.launcher.component

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.launcher.event.ShowStageRequestEvent
import kotlin.reflect.KClass

@Component
class EventPublisher(private val publisher: ApplicationEventPublisher) {
    fun sendShowRequest(target: KClass<out FxController>) {
        publisher.publishEvent(ShowStageRequestEvent(source = this::class, target = target))
    }
}