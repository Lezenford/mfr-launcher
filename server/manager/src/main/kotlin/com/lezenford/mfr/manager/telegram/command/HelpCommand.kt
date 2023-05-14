//package com.lezenford.mfr.manager.telegram.command
//
//import com.lezenford.mfr.manager.telegram.component.element.BotCommand
//import org.springframework.stereotype.Component
//import org.telegram.telegrambots.meta.api.methods.BotApiMethod
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage
//import org.telegram.telegrambots.meta.api.objects.Message
//
//@Component
//class HelpCommand : BotCommand() {
//    override val command: String = "help"
//
//    override fun execute(message: Message): BotApiMethod<*>? {
//        return SendMessage(message.chat.id.toString(), "Список команд:")
////    }
//}