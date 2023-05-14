package com.lezenford.mfr.launcher.exception

class TaskExecuteException : RuntimeException {
    constructor() : super()
    constructor(e: Exception) : super(e)
}