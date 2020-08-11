package ru.fullrest.mfr.plugins_configuration_utility.exception

class ApplicationStartException: RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}