package com.lezenford.mfr.launcher.service.factory

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.lezenford.mfr.javafx.component.FxController
import kotlin.reflect.KClass

@Profile("GUI")
@Component
class FxControllerFactory(
    private val applicationContext: ApplicationContext
) {
    fun <T : FxController> controller(type: KClass<T>): T = applicationContext.getBean(type.java)

    final inline fun <reified T : FxController> controller(): T = controller(T::class)
}