package ru.fullrest.mfr.plugins_configuration_utility.exception;

public class GameUpdateException extends Exception {
    public GameUpdateException(String message) {
        super(message);
    }

    public GameUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
