package com.lezenford.mfr.server.service

import com.lezenford.mfr.server.configuration.properties.ApplicationProperties
import com.lezenford.mfr.server.event.SendMessageEvent
import com.lezenford.mfr.server.model.entity.TelegramUser
import com.lezenford.mfr.server.service.model.TelegramUserService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import javax.annotation.PreDestroy

@Service
class NotificationService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val telegramUserService: TelegramUserService,
    private val applicationProperties: ApplicationProperties
) {
    private var successfullyStarted = false

    @EventListener(ApplicationReadyEvent::class)
    fun successfullyStarted() {
        telegramUserService.findAll().filter { it.role == TelegramUser.Role.ADMIN }.forEach {
            applicationEventPublisher.publishEvent(
                SendMessageEvent(
                    this,
                    SendMessage(it.id.toString(), "Сервер успешно запущен. Версия: ${applicationProperties.version}")
                )
            )
        }
        successfullyStarted = true
    }

    @PreDestroy
    fun preDestroy() {
        if (successfullyStarted) {
            telegramUserService.findAll().filter { it.role == TelegramUser.Role.ADMIN }.forEach {
                applicationEventPublisher.publishEvent(
                    SendMessageEvent(this, SendMessage(it.id.toString(), "Сервер остановлен"))
                )
            }
        }
    }
}