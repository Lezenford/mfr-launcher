package com.lezenford.mfr.server.security

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.server.configuration.History
import com.lezenford.mfr.server.service.model.OverviewService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*
import kotlin.coroutines.CoroutineContext

@Component
class UuidFilter(
    private val overviewService: OverviewService,
) : WebFilter, CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.History

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        exchange.request.headers[com.lezenford.mfr.common.IDENTITY_HEADER]?.firstOrNull()?.also { header ->
            launch {
                kotlin.runCatching {
                    UUID.fromString(header).also {
                        log.debug("Request from client $it")
                        overviewService.updateClientInfo(it.toString())
                    }
                }.onFailure {
                    when (it) {
                        is IllegalArgumentException -> log.error("Identity header is incorrect: $header")
                        else -> log.error("Update client info error", it)
                    }
                }
            }
        }
        return chain.filter(exchange)
    }

    companion object {
        private val log by Logger()
    }
}