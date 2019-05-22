package ru.fullrest.mfr.server.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Log4j2
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Не удалось загрузить файл")
public class UploadFileException extends Exception {
    public UploadFileException(String message) {
        super(message);
        log.error(this);
    }

    public UploadFileException(String message, Throwable cause) {
        super(message, cause);
        log.error(this);
    }
}
