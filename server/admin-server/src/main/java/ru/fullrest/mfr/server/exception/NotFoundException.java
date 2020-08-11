package ru.fullrest.mfr.server.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Log4j2
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Данная страница не существует")
public class NotFoundException extends Exception {
    public NotFoundException(String message) {
        super(message);
        log.error(this);
    }
}
