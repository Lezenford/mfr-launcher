package ru.fullrest.mfr.launcher.exception

import org.springframework.beans.factory.ObjectFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import ru.fullrest.mfr.common.exception.ServerMaintenanceException
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.javafx.controller.NotificationController
import java.io.IOException
import kotlin.system.exitProcess

@Component
class ExceptionHandler(
    private val notificationFactory: ObjectFactory<NotificationController>,
    private val applicationStatus: ApplicationStatus
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread?, e: Throwable) {
        val exception = removeFxTargetInvocationException(e)

        log.error(exception)

        val alertController = kotlin.runCatching { notificationFactory.`object` }.getOrNull()

        when (exception) {
            is NotEnoughSpaceException -> alertController?.info(description = exception.message)
            is RestClientException -> alertController?.info(description = "Ошибка подключения к серверу").also {
                applicationStatus.onlineMode.value = false
            }
            is IOException -> {
                if (exception.message?.contains("error=740") == true) {
                    alertController?.info(
                        title = "Недостаточно прав",
                        description = "Перезапустите конфигуратор от имени администратора"
                    )
                } else {
                    alertController?.info(
                        title = "Ошибка",
                        description = e.message ?: "Пожалуйста, свяжитесь с разработчиками приложения"
                    )
                }
            }
            is StartApplicationException -> exitProcess(0)
            is ExternalApplicationException -> alertController?.info(
                description = exception.message ?: DEFAULT_DESCRIPTION
            )
            is ApplicationException -> alertController?.error(
                description = exception.message ?: DEFAULT_DESCRIPTION
            )
            is ServerMaintenanceException -> alertController?.info(
                description = "Сервер в процессе обновления. Пожалуйста, попробуйте позже"
            )
            is OnlineModException -> alertController?.info(
                description = "Сервер недоступен. Лаунчер переведет в offline режим"
            ).also {
                applicationStatus.onlineMode.value = false
            }
            else -> alertController?.info(description = DEFAULT_DESCRIPTION)
        }
    }

    private fun removeFxTargetInvocationException(e: Throwable): Throwable {
        if (e is java.lang.RuntimeException && e.message == "java.lang.reflect.InvocationTargetException") {
            e.cause?.also {
                if (it is java.lang.reflect.InvocationTargetException && it.message == null) {
                    return it.cause ?: e
                }
            } ?: return e
        }
        return e
    }

    companion object {
        private val log by Logger()
        private const val DEFAULT_DESCRIPTION = "Произошла ошибка"
    }
}