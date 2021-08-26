package ru.fullrest.mfr.launcher.event

import org.springframework.context.ApplicationEvent
import ru.fullrest.mfr.javafx.component.FxController
import kotlin.reflect.KClass

class ShowStageRequestEvent<T : FxController>(
    source: Any,
    val target: KClass<T>,
    val init: (T) -> Unit = {}
) : ApplicationEvent(source)