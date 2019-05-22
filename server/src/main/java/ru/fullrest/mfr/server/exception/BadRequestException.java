package ru.fullrest.mfr.server.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Log4j2
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Неверный запрос")
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
        log.error(this);
    }
}
