package ru.fullrest.mfr.launcher.exception

class ApplicationUpdateException: Exception{
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}