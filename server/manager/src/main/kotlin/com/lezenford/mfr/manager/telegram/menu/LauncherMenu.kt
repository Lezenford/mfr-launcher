package com.lezenford.mfr.manager.telegram.menu

import com.lezenford.mfr.manager.configuration.MODULE_CACHE
import com.lezenford.mfr.manager.extensions.sendMessage
import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.service.StorageService
import com.lezenford.mfr.manager.telegram.command.LauncherDistributiveCommand
import com.lezenford.mfr.manager.telegram.component.ReceiveMessageHandler
import com.lezenford.mfr.manager.telegram.component.element.Menu
import com.lezenford.mfr.manager.telegram.component.element.MenuBuilder
import com.lezenford.mfr.manager.telegram.component.element.MenuItem
import com.lezenford.mfr.manager.telegram.component.element.menu
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.asFlow
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import com.lezenford.mfr.common.protocol.enums.SystemType

@Component
class LauncherMenu(
    private val storageService: StorageService,
    private val receiveMessageHandler: ReceiveMessageHandler
) : Menu() {
    override val identity: Type = Type.LAUNCHER
    override val permissions: List<TelegramUser.Role> = listOf(TelegramUser.Role.ADMIN)

    @get:Cacheable(value = [MODULE_CACHE], key = "'LauncherCallbackModule'")
    override val firstMenuItem: Deferred<MenuItem>
        get() = menu("Для управлением версиями лаунчера воспользуйтесь командой /${LauncherDistributiveCommand.command}") {
            text = "Управление версиями лаунчера"
            buttons(SystemType.values().asFlow()) { system ->
                id = system.ordinal
                text = system.name
                message = {
                    val launcher = storageService.findLauncher(system)
                    text = "Версия для $system. Версия: ${launcher.version}. Имя файла: ${launcher.fileName}"
                    button {
                        text = "Указать имя"
                        message = {
                            text = "Введите имя файла"
                        }
                        action = {
                            receiveMessageHandler[it.message.chatId] = {
                                when (storageService.updateLauncher(
                                    system,
                                    version = "",
                                    fileName = message.text
                                )) {
                                    StorageService.Result.SUCCESS -> sendMessage("Имя файла успешно установлено")
                                    else -> sendMessage("В процессе операции произошла ошибка")
                                }
                            }
                        }
                    }
                    button {
                        text = "Указать версию"
                        message = {
                            text = "Введите номер версии"
                        }
                        action = {
                            receiveMessageHandler[it.message.chatId] = {
                                when (storageService.updateLauncher(system, version = message.text)) {
                                    StorageService.Result.SUCCESS -> sendMessage("Версия ${message.text} успешно установлена для версии $system")
                                    else -> sendMessage("В процессе операции произошла ошибка")
                                }
                            }
                        }
                    }
                    button(MenuBuilder.Action.REFRESH)
                    button(MenuBuilder.Action.BACK)
                    button(MenuBuilder.Action.CLOSE)
                }
            }
            button(MenuBuilder.Action.CLOSE)
        }
}