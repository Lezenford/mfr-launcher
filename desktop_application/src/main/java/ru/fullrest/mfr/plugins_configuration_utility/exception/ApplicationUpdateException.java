package ru.fullrest.mfr.plugins_configuration_utility.exception;

public class ApplicationUpdateException extends Exception {
    public ApplicationUpdateException(String message) {
        super(message);
    }

    public ApplicationUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
