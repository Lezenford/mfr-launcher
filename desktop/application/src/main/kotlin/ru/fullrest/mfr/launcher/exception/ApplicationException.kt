package ru.fullrest.mfr.launcher.exception

/**
 * Base application exception
 */
open class ApplicationException : RuntimeException {
    constructor(e: Exception) : super(e)
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}