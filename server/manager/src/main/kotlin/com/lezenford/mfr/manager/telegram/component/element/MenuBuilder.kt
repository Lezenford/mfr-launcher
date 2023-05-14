package com.lezenford.mfr.manager.telegram.component.element

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import org.telegram.telegrambots.meta.api.objects.CallbackQuery

@DslMarker
annotation class MenuDsl

@Suppress("DeferredIsResult")
fun Menu.menu(closeMessage: String, init: suspend MenuBuilder.MessageDsl.() -> Unit): Deferred<MenuItem> =
    CoroutineScope(Dispatchers.Default).async { MenuGenerator(identity.ordinal, closeMessage, init).build() }

private class MenuGenerator(
    private val menuId: Int,
    private val closeMessage: String,
    private val init: suspend MenuBuilder.MessageDsl.() -> Unit
) {

    suspend fun build(): MenuItem = MessageDslConverter(menuId).also { it.init() }.build()

    private val closeButton: Button = ButtonImp(
        text = "Закрыть",
        menuItem = MenuItemImpl(
            identity = intArrayOf(menuId, Int.MIN_VALUE),
            textGenerator = { closeMessage }
        )
    )

    private class MenuItemImpl(
        override val identity: IntArray,
        private val textGenerator: suspend () -> String,
        buttons: MutableList<Button> = mutableListOf()
    ) : MenuItem {
        override val text: Deferred<String>
            get() = CoroutineScope(Dispatchers.Default).async { textGenerator() }
        override val buttons: List<Button> by lazy { buttons.toList() }
    }

    private class ButtonImp(
        override val text: String,
        override val menuItem: MenuItem,
        override val action: suspend (CallbackQuery) -> Unit = {}
    ) : Button {
        override fun toString(): String = text
    }

    private inner class MessageDslConverter(id: Int, parentMenuItem: MenuItem? = null) :
        MenuBuilder.MessageDsl(id, parentMenuItem) {
        suspend fun build(): MenuItem {
            var buttonCounter = 1
            val messageButtons = mutableListOf<Button>()
            val message = MenuItemImpl(
                buttons = messageButtons, identity = identity, textGenerator = dynamicText
            )
            buttons.map {
                ButtonDslConverter(buttonCounter++, message).also(it).build()
            }.forEach(messageButtons::add)
            if (refreshable) {
                messageButtons.add(
                    ButtonImp(
                        text = "Обновить",
                        menuItem = message
                    )
                )
            }
            if (backable && parentMenuItem != null) {
                messageButtons.add(
                    ButtonImp(
                        text = "Назад",
                        menuItem = parentMenuItem
                    )
                )
            }
            if (closable) {
                messageButtons.add(closeButton)
            }
            return message
        }
    }

    private inner class ButtonDslConverter(id: Int, parentMenuItem: MenuItem) :
        MenuBuilder.ButtonDsl(id, parentMenuItem) {
        suspend fun build(): Button = ButtonImp(
            text = text,
            menuItem = MessageDslConverter(id, parentMenuItem).also { it.message() }.build(),
            action = action
        )
    }
}

object MenuBuilder {
    @MenuDsl
    sealed class MessageDsl(private val id: Int, protected val parentMenuItem: MenuItem?) {
        lateinit var text: String
        var dynamicText: suspend () -> String = { text }
        protected val buttons: MutableList<ButtonDsl.() -> Unit> = mutableListOf()
        protected val identity: IntArray
            get() = parentMenuItem?.identity?.copyInto(IntArray(parentMenuItem.identity.size + 1))?.also {
                it[it.lastIndex] = id
            } ?: intArrayOf(id)

        protected var refreshable = false
        protected var backable = false
        protected var closable = false

        fun button(builder: ButtonDsl.() -> Unit) {
            buttons.add(builder)
        }

        fun button(action: Action) {
            when (action) {
                Action.REFRESH -> refreshable = true
                Action.BACK -> backable = true
                Action.CLOSE -> closable = true
            }
        }

        suspend fun <T> buttons(items: Flow<T>, builder: ButtonDsl.(T) -> Unit) {
            items.collect {
                buttons.add {
                    builder(it)
                }
            }
        }
    }

    @MenuDsl
    sealed class ButtonDsl(var id: Int, protected val parentMenuItem: MenuItem) {
        lateinit var text: String
        var action: suspend (CallbackQuery) -> Unit = {}
        var message: suspend MessageDsl.() -> Unit = {
            text = "Команда успешно выполнена"
        }
    }

    enum class Action {
        REFRESH, BACK, CLOSE
    }
}
