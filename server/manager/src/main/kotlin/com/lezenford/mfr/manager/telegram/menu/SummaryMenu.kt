package com.lezenford.mfr.manager.telegram.menu

import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.service.StorageService
import com.lezenford.mfr.manager.telegram.command.SummaryCommand
import com.lezenford.mfr.manager.telegram.component.element.Menu
import com.lezenford.mfr.manager.telegram.component.element.MenuBuilder
import com.lezenford.mfr.manager.telegram.component.element.menu
import org.springframework.stereotype.Component

@Component
class SummaryMenu(
    private val storageService: StorageService
) : Menu() {
    override val identity: Type = Type.SUMMARY
    override val permissions: List<TelegramUser.Role> = listOf(TelegramUser.Role.ADMIN)
    override val firstMenuItem
        get() = menu("Для отображения статистики воспользуйтесь командой /${SummaryCommand.command}") {
            val summary = storageService.summary()

            dynamicText = {
                summary.run {
                    """Уникальных пользователей всего\за месяц: ${users.total}\${users.lastMonth}

        |Скачивали игру всего\за месяц: ${gameDownloads.total}\${gameDownloads.lastMonth}

        |Скачивали дополнительный контент всего\за месяц: ${extraContentDownload.total}\${extraContentDownload.lastMonth}

        |Скачивали опциональный контент всего\за месяц:
        |${
                        optionalContentDownload.map { (key, value) ->
                            "$key: ${value.total}\\${value.lastMonth}"
                        }.joinToString("\n")
                    } """.trimMargin()
                }
            }
            button(MenuBuilder.Action.REFRESH)
        }
}