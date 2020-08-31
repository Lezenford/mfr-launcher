package ru.fullrest.mfr.plugins_configuration_utility.exception

class RestException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}