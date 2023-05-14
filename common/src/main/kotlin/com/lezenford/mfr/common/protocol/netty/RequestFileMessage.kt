package com.lezenford.mfr.common.protocol.netty

import java.util.*

abstract class RequestFileMessage : Message() {
    abstract val clientId: UUID
}