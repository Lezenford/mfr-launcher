package ru.fullrest.mfr.plugins_configuration_utility.exception

/**
 * Exception for event before start UI
 * Closed app
 */
class StartApplicationException : ApplicationException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}