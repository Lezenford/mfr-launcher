package com.lezenford.mfr.launcher.exception

class ServerConnectionException : RuntimeException {
    constructor() : super()
    constructor(e: Throwable) : super(e)
}