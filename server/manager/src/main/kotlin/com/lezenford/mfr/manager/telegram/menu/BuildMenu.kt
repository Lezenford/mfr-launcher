package com.lezenford.mfr.manager.telegram.menu

import com.lezenford.mfr.manager.configuration.MODULE_CACHE
import com.lezenford.mfr.manager.extensions.sendMessage
import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.service.StorageService
import com.lezenford.mfr.manager.telegram.command.BuildDistributiveCommand
import com.lezenford.mfr.manager.telegram.component.ReceiveMessageHandler
import com.lezenford.mfr.manager.telegram.component.element.Menu
import com.lezenford.mfr.manager.telegram.component.element.MenuBuilder
import com.lezenford.mfr.manager.telegram.component.element.MenuItem
import com.lezenford.mfr.manager.telegram.component.element.menu
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery

@Component
class BuildMenu(
    private val receiveMessageHandler: ReceiveMessageHandler,
    private val storageService: StorageService
) : Menu() {
    override val identity: Type = Type.BUILD
    override val permissions: List<TelegramUser.Role> = listOf(TelegramUser.Role.ADMIN)

    @get:Cacheable(value = [MODULE_CACHE], key = "'BuildCallbackModule'")
    override val firstMenuItem: Deferred<MenuItem>
        get() = menu("Для управления версия игры воспользуйтесь командой /${BuildDistributiveCommand.command}") {
            text = "Управление версиями сборки"
            button {
                text = "Создать новую"
                message = {
                    text = "Введите название новой версии"
                }
                action = createBuildAction()
            }
            button {
                text = "Управлять существующей"
                message = {
                    text = "Выберите версию игры"
                    buttons(storageService.findAllBuilds()) { build ->
                        id = build.id
                        text = build.name
                        message = {
                            text = "Версия ${build.name}\nДата последнего обновления: ${build.lastUpdate}"
                            if (build.default.not()) {
                                button {
                                    text = "Сделать сборкой по умолчанию"
                                    message = {
                                        dynamicText = {
                                            when (storageService.setDefaultBuild(build.id)) {
                                                StorageService.Result.SUCCESS -> "Сборка используется по умолчанию"
                                                else -> "Ошибка выполнения операции"
                                            }
                                        }
                                    }
                                }
                            }
                            button {
                                text = "Запустить обновление"
                                message = {
                                    dynamicText = {
                                        when (storageService.updateBuild(build.id)) {
                                            StorageService.Result.SUCCESS -> "Запущено обновление версии ${build.name}"
                                            StorageService.Result.CONFLICT -> "Выполнение обновления уже выполняется"
                                            else -> "Ошибка выполнения операции"
                                        }
                                    }
                                }
                            }
                            button(MenuBuilder.Action.BACK)
                        }
                    }
                    button(MenuBuilder.Action.BACK)
                    button(MenuBuilder.Action.CLOSE)
                }
            }
            button(MenuBuilder.Action.CLOSE)
        }

    private fun createBuildAction(): suspend (CallbackQuery) -> Unit = { callbackQuery ->
        receiveMessageHandler[callbackQuery.message.chatId] = {
            val build = message.text
            storageService.findAllBuilds().firstOrNull { it.name == build }?.let {
                sendMessage("Версия с таким именем уже существует. Процесс создания прерван")
            } ?: sendMessage("Введите имя ветки").also {
                receiveMessageHandler[callbackQuery.message.chatId] = {
                    val branch = message.text
                    storageService.createBuild(name = build, branch = branch).run {
                        when (this) {
                            StorageService.Result.SUCCESS -> sendMessage("Версия $build успешно создана")
                            StorageService.Result.CONFLICT -> sendMessage("Версия $build уже существует")
                            StorageService.Result.ERROR -> sendMessage("Ошибка при создании сервиса. Попробуйте позднее")
                        }
                    }
                }
            }
        }
    }
}