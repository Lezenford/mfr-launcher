package ru.fullrest.mfr.plugins_configuration_utility.exception

import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.createAlert
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import java.io.IOException
import kotlin.system.exitProcess

@Component
class ExceptionHandler : Thread.UncaughtExceptionHandler, Loggable {
    override fun uncaughtException(t: Thread?, e: Throwable) {
        val exception = removeFxTargetInvocationException(e)

        log().error(exception)

        val alertController = createAlert(Stage.getWindows().find { it.isShowing })

        CoroutineScope(Dispatchers.JavaFx).launch {
            when (exception) {
                is RestClientException -> alertController.error(description = "Ошибка подключения к серверу")
                is IOException -> {
                    if (exception.message?.contains("error=740") == true) {
                        alertController.info(
                            title = "Недостаточно прав",
                            description = "Перезапустите конфигуратор от имени администратора"
                        )
                    } else {
                        alertController.info(
                            title = "Ошибка",
                            description = e.message ?: "Пожалуйста, свяжитесь с разработчиками приложения"
                        )
                    }
                }
                is StartApplicationException -> exitProcess(0)
                is ExternalApplicationException -> alertController.info(
                    description = exception.message ?: DEFAULT_DESCRIPTION
                )
                is ApplicationException -> alertController.error(
                    description = exception.message ?: DEFAULT_DESCRIPTION
                )
                else -> alertController.info(description = DEFAULT_DESCRIPTION)
            }
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
        private const val DEFAULT_DESCRIPTION = "Произошла ошибка,\nприложение будет закрыто"
    }
}