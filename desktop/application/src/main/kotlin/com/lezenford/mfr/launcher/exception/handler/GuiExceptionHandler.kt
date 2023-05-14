package com.lezenford.mfr.launcher.exception.handler

import com.lezenford.mfr.launcher.exception.ApplicationException
import com.lezenford.mfr.launcher.exception.ExternalApplicationException
import com.lezenford.mfr.launcher.exception.NotEnoughSpaceException
import com.lezenford.mfr.launcher.exception.ServerConnectionException
import com.lezenford.mfr.launcher.exception.StartApplicationException
import com.lezenford.mfr.launcher.javafx.controller.NotificationController
import org.springframework.beans.factory.ObjectFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.lezenford.mfr.common.exception.ServerMaintenanceException
import kotlin.system.exitProcess

@Profile("GUI")
@Component
class GuiExceptionHandler(
    private val notificationFactory: ObjectFactory<NotificationController>
) : AbstractExceptionHandler() {
    override fun clearException(e: Throwable): Throwable {
        if (e is java.lang.RuntimeException && e.message == "java.lang.reflect.InvocationTargetException") {
            e.cause?.also {
                if (it is java.lang.reflect.InvocationTargetException && it.message == null) {
                    return it.cause ?: e
                }
            } ?: return e
        }
        return e
    }

    override fun showError(e: Throwable) {
        val alertController = kotlin.runCatching { notificationFactory.`object` }.getOrNull()

        when (e) {
            is NotEnoughSpaceException -> alertController?.info(description = e.message)

            is StartApplicationException -> exitProcess(0)
            is ExternalApplicationException -> alertController?.info(
                description = e.message ?: DEFAULT_DESCRIPTION
            )

            is ApplicationException -> alertController?.error(
                description = e.message ?: DEFAULT_DESCRIPTION
            )

            is com.lezenford.mfr.common.exception.ServerMaintenanceException -> alertController?.info(
                description = "Сервер в процессе обновления. Пожалуйста, повторите попытку позднее"
            )

            is ServerConnectionException -> alertController?.info(
                description = "Сервер недоступен"
            )

            else -> alertController?.info(description = DEFAULT_DESCRIPTION)
        }
    }
}