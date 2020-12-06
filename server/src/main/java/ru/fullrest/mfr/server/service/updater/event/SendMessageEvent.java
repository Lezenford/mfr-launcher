package ru.fullrest.mfr.server.service.updater.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

public class SendMessageEvent extends ApplicationEvent {
    @Getter
    private final BotApiMethod<?> message;

    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source  the object on which the event initially occurred or with
     *                which the event is associated (never {@code null})
     * @param message message for send to users
     */
    public SendMessageEvent(Object source, BotApiMethod<?> message) {
        super(source);
        this.message = message;
    }
}
