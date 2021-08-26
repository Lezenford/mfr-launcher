package com.lezenford.mfr.server.telegram.callback_module

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.extensions.sendMessage
import com.lezenford.mfr.server.model.entity.Launcher
import com.lezenford.mfr.server.service.model.LauncherService
import com.lezenford.mfr.server.telegram.component.CallbackMessageHandler
import com.lezenford.mfr.server.telegram.component.CallbackModule
import com.lezenford.mfr.server.telegram.component.element.Keyboard
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Chat
import ru.fullrest.mfr.common.api.SystemType
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.common.extensions.toPath
import kotlin.io.path.exists
import kotlin.io.path.fileSize

@Component
@PreAuthorize("hasAuthority('ADMIN')")
class LauncherCallbackModule(
    private val launcherService: LauncherService,
    private val callbackMessageHandler: CallbackMessageHandler,
    private val serverSettingProperties: ServerSettingProperties
) : CallbackModule() {
    override val type: Type = Type.LAUNCHER

    override fun init(): Chat.() -> BotApiMethod<*>? = {
        sendMessage(
            text = "Управление версиями игры",
            keyboard = Keyboard {
                lines(SystemType.values().toList()) {
                    text = it.name
                    callbackData = callbackData(Event.SELECT_LAUNCHER, mapOf(SYSTEM_TYPE_PROPERTY to it.name))
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
            Event.SELECT_LAUNCHER -> {
                val systemType = data.details[SYSTEM_TYPE_PROPERTY]?.let { SystemType.valueOf(it) }
                    ?: throw IllegalArgumentException("Incorrect launcher system ${data.details[SYSTEM_TYPE_PROPERTY]}")
                val launcher = launcherService.findBySystem(systemType)
                editMessage(
                    text = "Версия для $systemType. Версия: ${launcher?.version}. Имя файла: ${launcher?.fileName}",
                    keyboard = Keyboard {
                        line {
                            text = "Указать имя"
                            callbackData =
                                callbackData(Event.SET_LAUNCHER_NAME, mapOf(SYSTEM_TYPE_PROPERTY to systemType.name))
                        }
                        line {
                            text = "Укажите версию"
                            callbackData =
                                callbackData(Event.SET_LAUNCHER_VERSION, mapOf(SYSTEM_TYPE_PROPERTY to systemType.name))
                        }
                        line {
                            text = "Закрыть"
                            callbackData = callbackData(Event.CLOSE)
                        }
                    }
                )
            }
            Event.SET_LAUNCHER_NAME -> editMessage(text = "Введите имя файла").also {
                callbackMessageHandler[query.message.chatId] = {
                    val systemType = data.details[SYSTEM_TYPE_PROPERTY]?.let { SystemType.valueOf(it) }
                        ?: throw IllegalArgumentException("Incorrect launcher system ${data.details[SYSTEM_TYPE_PROPERTY]}")
                    val launcher = launcherService.findBySystem(systemType)?.also { it.fileName = message.text }
                        ?: Launcher(
                            system = systemType,
                            version = "",
                            md5 = ByteArray(0),
                            fileName = message.text,
                            size = 0
                        )
                    launcherService.save(launcher)
                    sendMessage("Имя файла ${message.text} успешно установлено для версии $systemType")
                }
            }
            Event.SET_LAUNCHER_VERSION -> editMessage(text = "Введите номер версии").also {
                callbackMessageHandler[query.message.chatId] = {
                    val systemType = data.details[SYSTEM_TYPE_PROPERTY]?.let { SystemType.valueOf(it) }
                        ?: throw IllegalArgumentException("Incorrect launcher system ${data.details[SYSTEM_TYPE_PROPERTY]}")
                    val launcher = launcherService.findBySystem(systemType)
                        ?: throw IllegalArgumentException("Launcher doesn't exist")
                    launcher.version = message.text
                    launcher.md5 = serverSettingProperties.launcherFolder.toPath().resolve(systemType.toString())
                        .resolve(launcher.fileName).also {
                            if (it.exists().not()) {
                                throw IllegalArgumentException("Launcher file doesn't exist")
                            }
                            launcher.size = it.fileSize()
                        }.md5()
                    launcherService.save(launcher)
                    sendMessage("Версия ${message.text} успешно установлена для версии $systemType")
                }
            }
            Event.CLOSE -> editMessage("Для управлением версиями лаунчера воспользуйтесь командой /launcher")
        }
    }

    enum class Event {
        CLOSE, SELECT_LAUNCHER, SET_LAUNCHER_NAME, SET_LAUNCHER_VERSION
    }

    companion object {
        private const val SYSTEM_TYPE_PROPERTY = "1"
        private val log by Logger()
    }
}