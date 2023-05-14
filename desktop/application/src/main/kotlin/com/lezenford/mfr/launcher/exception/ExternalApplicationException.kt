package com.lezenford.mfr.launcher.exception

/**
 * Exception from external event (Windows runtime exception for example)
 * Not critical for application and not closable
 */
class ExternalApplicationException : ApplicationException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}