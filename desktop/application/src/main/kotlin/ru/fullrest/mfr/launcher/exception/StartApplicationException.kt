package ru.fullrest.mfr.launcher.exception

/**
 * Exception for event before start UI
 * Closed app
 */
class StartApplicationException : ApplicationException {
    constructor(e: Exception) : super(e)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}