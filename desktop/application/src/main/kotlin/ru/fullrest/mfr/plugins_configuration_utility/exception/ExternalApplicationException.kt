package ru.fullrest.mfr.plugins_configuration_utility.exception

/**
 * Exception from external event (Windows runtime exception for example)
 * Not critical for application and not closable
 */
class ExternalApplicationException : ApplicationException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}