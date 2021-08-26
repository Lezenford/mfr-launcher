package com.lezenford.mfr.server.telegram.callback_module

import com.lezenford.mfr.server.extensions.sendMessage
import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.service.UpdaterProcessorExecutor
import com.lezenford.mfr.server.service.model.BuildService
import com.lezenford.mfr.server.telegram.component.CallbackMessageHandler
import com.lezenford.mfr.server.telegram.component.CallbackModule
import com.lezenford.mfr.server.telegram.component.element.Keyboard
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Chat
import ru.fullrest.mfr.common.extensions.Logger

@Component
@PreAuthorize("hasAuthority('ADMIN')")
class BuildCallbackModule(
    private val updaterProcessorExecutor: UpdaterProcessorExecutor,
    private val callbackMessageHandler: CallbackMessageHandler,
    private val buildService: BuildService
) : CallbackModule() {
    override val type: Type = Type.BUILD

    override fun init(): Chat.() -> BotApiMethod<*>? = {
        sendMessage(
            text = "Управление версиями лаунчера",
            keyboard = Keyboard {
                line {
                    text = "Создать новый"
                    callbackData = callbackData(Event.CREATE)
                }
                line {
                    text = "Управлять существующей"
                    callbackData = callbackData(Event.MANAGE)
                }

                line {
                    text = "Закрыть"
                    callbackData = callbackData(Event.CLOSE)
                }
            }
        )
    }

    override fun process(): Process.() -> BotApiMethod<*>? = {
        when (Event.values()[data.event]) {
            Event.CREATE -> editMessage("Введите название новой версии").also {
                callbackMessageHandler[query.message.chatId] = {
                    val build = message.text
                    buildService.findByName(build)?.let {
                        sendMessage("Версия с таким именем уже существует. Процесс создания прерван")
                    } ?: sendMessage("Введите имя ветки").also {
                        callbackMessageHandler[query.message.chatId] = {
                            val branch = message.text
                            buildService.save(Build(name = build, branch = branch)).run {
                                sendMessage("Версия $build успешно создана")
                            }
                        }
                    }
                }
            }
            Event.MANAGE -> editMessage(
                text = "Выберите версию игры",
                keyboard = Keyboard {
                    lines(buildService.findAll()) {
                        text = it.name
                        callbackData = callbackData(Event.SELECT_BUILD, mapOf(BUILD_ID_PROPERTY to it.id.toString()))
                    }
                    line {
                        text = "Закрыть"
                        callbackData = callbackData(Event.CLOSE)
                    }
                }
            )
            Event.REFRESH,
            Event.SELECT_BUILD -> {
                val build = data.details[BUILD_ID_PROPERTY]?.toInt()?.let {
                    buildService.findById(it)
                } ?: throw IllegalArgumentException("Incorrect build id")
                editMessage(
                    text = "Версия `${build.name}`\nДата последнего обновления: ${build.lastUpdateDate}",
                    keyboard = Keyboard {
                        line {
                            text = "Обновить"
                            callbackData =
                                callbackData(Event.UPDATE_BUILD, mapOf(BUILD_ID_PROPERTY to build.id.toString()))
                        }
                        if (build.default.not()) {
                            line {
                                text = "Сделать сборкой по умолчанию"
                                callbackData =
                                    callbackData(Event.DEFAULT, mapOf(BUILD_ID_PROPERTY to build.id.toString()))
                            }
                        }
                        line {
                            text = "Назад"
                            callbackData = callbackData(Event.MANAGE)
                        }
                    }
                )
            }
            Event.UPDATE_BUILD -> {
                val build = data.details[BUILD_ID_PROPERTY]?.toInt()?.let {
                    buildService.findById(it)
                } ?: throw IllegalArgumentException("Incorrect build id")
                updaterProcessorExecutor.updateBuild(build).takeIf { it }?.run {
                    editMessage("Запущено обновление версии ${build.name}")
                } ?: editMessage(
                    text = "Не удалось запустить обновление - сервер уже выполняет операцию обновления, попробуйте позже" +
                            "\n\nВерсия `${build.name}`\nДата последнего обновления: ${build.lastUpdateDate}",
                    keyboard = Keyboard {
                        line {
                            text = "Обновить"
                            callbackData =
                                callbackData(Event.UPDATE_BUILD, mapOf(BUILD_ID_PROPERTY to build.id.toString()))
                        }
                        line {
                            text = "Назад"
                            callbackData = callbackData(Event.MANAGE)
                        }
                    }
                )
            }
            Event.DEFAULT -> {
                val buildId = data.details[BUILD_ID_PROPERTY]?.toInt()
                    ?: throw IllegalArgumentException("Incorrect build id")
                buildService.updateDefault(buildId)
                editMessage("Сборка используется по умолчанию")
            }
            Event.CLOSE -> editMessage("Для управлением версиями воспользуйтесь командой /build")
        }
    }

    enum class Event {
        CLOSE, CREATE, MANAGE, SELECT_BUILD, UPDATE_BUILD, REFRESH, DEFAULT
    }

    companion object {
        private const val BUILD_ID_PROPERTY = "1"
        private val log by Logger()
    }
}