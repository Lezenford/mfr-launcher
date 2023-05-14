package com.lezenford.mfr.manager.telegram.component.element

import com.lezenford.mfr.manager.model.entity.TelegramUser
import kotlinx.coroutines.Deferred
import org.telegram.telegrambots.meta.api.objects.CallbackQuery

abstract class Menu {
    abstract val identity: Type
    abstract val permissions: List<TelegramUser.Role>

    abstract val firstMenuItem: Deferred<MenuItem>

    enum class Type {
        USER_ROLE, SUMMARY, BUILD, LAUNCHER
    }
}

interface MenuItem {
    val text: Deferred<String>
    val buttons: List<Button>
    val identity: IntArray
}

interface Button {
    val text: String
    val action: suspend (CallbackQuery) -> Unit
    val menuItem: MenuItem
}

