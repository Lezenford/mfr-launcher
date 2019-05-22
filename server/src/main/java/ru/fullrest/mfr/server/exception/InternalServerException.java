package ru.fullrest.mfr.server.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Log4j2
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Ошибка сервера")
public class InternalServerException extends Exception {
    public InternalServerException(String message) {
        super(message);
        log.error(this);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
        log.error(this);
    }

    public InternalServerException(Throwable cause) {
        super(cause);
        log.error(this);
    }
}
