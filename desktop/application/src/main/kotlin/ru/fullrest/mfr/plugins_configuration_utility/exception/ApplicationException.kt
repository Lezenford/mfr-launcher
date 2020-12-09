package ru.fullrest.mfr.plugins_configuration_utility.exception

/**
 * Base application exception
 */
open class ApplicationException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}