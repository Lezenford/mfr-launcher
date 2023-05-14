package com.lezenford.mfr.common.protocol.netty

import java.util.*

class ServerExceptionMessage(
    val text: String? = null,
    val uuid: UUID? = null
) : Message()