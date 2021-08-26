package ru.fullrest.mfr.launcher.exception

class GameUpdateException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}