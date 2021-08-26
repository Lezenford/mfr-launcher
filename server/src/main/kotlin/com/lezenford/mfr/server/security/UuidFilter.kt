package com.lezenford.mfr.server.security

import com.lezenford.mfr.server.service.model.ClientService
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.fullrest.mfr.common.IDENTITY_HEADER
import ru.fullrest.mfr.common.extensions.Logger
import java.util.*
import java.util.concurrent.ExecutorService
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class UuidFilter(
    private val clientService: ClientService,
    private val historyUpdateExecutor: ExecutorService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.getHeader(IDENTITY_HEADER)?.also { header ->
            historyUpdateExecutor.execute {
                kotlin.runCatching {
                    UUID.fromString(header)?.also {
                        log.info("Request from client $it")
                        clientService.updateClientInfo(it.toString())
                    }
                }.onFailure {
                    when (it) {
                        is IllegalArgumentException -> log.error("Identity header is incorrect: $header", it)
                        else -> log.error("Update client info error", it)
                    }
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    companion object {
        private val log by Logger()
    }
}