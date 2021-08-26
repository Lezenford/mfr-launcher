package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.service.StorageService
import com.lezenford.mfr.server.telegram.callback_module.BuildCallbackModule
import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class BuildDistributiveCommand(
    private val storageService: StorageService,
    private val buildCallbackModule: BuildCallbackModule
) : BotCommand() {
    override val command: String = "build"
    override val description: String = "Настройка игры"
    override val publish: Boolean = true

    @PreAuthorize("hasAuthority('ADMIN')")
    override fun execute(message: Message): BotApiMethod<*>? {
//        CoroutineScope(Dispatchers.IO).launch { fileService.createBuild() }
        return buildCallbackModule.init(message.chat)
    }
}