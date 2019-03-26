package ru.fullrest.mfr.server.exception;

public class UploadFileException extends Exception {
    public UploadFileException(String message) {
        super(message);
    }

    public UploadFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
