package com.lezenford.mfr.launcher.service.initiator

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.exception.handler.AbstractExceptionHandler
import com.lezenford.mfr.launcher.javafx.controller.LauncherController
import com.lezenford.mfr.launcher.javafx.controller.StartController
import com.lezenford.mfr.launcher.javafx.controller.WelcomeController
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.service.OpenMwService
import com.lezenford.mfr.launcher.service.factory.FxControllerFactory
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.provider.RSocketProvider
import com.lezenford.mfr.launcher.service.provider.RestProvider
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("GUI")
@Component
class JavaFxApplicationInitiator(
    override val exceptionHandler: AbstractExceptionHandler,
    override val applicationProperties: ApplicationProperties,
    override val gameProperties: GameProperties,
    override val propertyService: PropertiesService,
    override val restProvider: RestProvider,
    override val openMwService: OpenMwService,
    override val rSocketProvider: RSocketProvider,
    override val objectMapper: ObjectMapper,
    private val fxControllerFactory: FxControllerFactory
) : InitApplicationInitiator() {
    override suspend fun complete() {
        fxControllerFactory.controller<StartController>().close()
        fxControllerFactory.controller<LauncherController>().show()
        propertyService.findByKey(Properties.Key.FIRST_START) ?: kotlin.run {
            fxControllerFactory.controller<WelcomeController>().show()
            propertyService.save(Properties(key = Properties.Key.FIRST_START))
        }
    }
}