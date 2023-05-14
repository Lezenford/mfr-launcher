package com.lezenford.mfr.common.exception

class ServerMaintenanceException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
}