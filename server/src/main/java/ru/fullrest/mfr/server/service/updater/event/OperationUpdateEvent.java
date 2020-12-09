package ru.fullrest.mfr.server.service.updater.event;

import org.springframework.context.ApplicationEvent;

public class OperationUpdateEvent extends ApplicationEvent {

    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public OperationUpdateEvent(Object source) {
        super(source);
    }
}

