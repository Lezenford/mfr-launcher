package com.lezenford.mfr.common.extensions

import org.springframework.messaging.rsocket.RSocketRequester

data class RSocketClientSpec internal constructor(
    val data: Any,
    val route: String,
)

fun RSocketRequester.spec(spec: RSocketClientSpec): RSocketRequester.RetrieveSpec {
    return this.route(spec.route).data(spec.data)
}
