package com.lezenford.mfr.server.configuration

import com.lezenford.mfr.server.security.SecurityContextRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration(
    private val securityContextRepository: SecurityContextRepository
) {

    @Bean
    fun securityWebFilterChain(security: ServerHttpSecurity): SecurityWebFilterChain =
        security
            .cors().disable()
            .csrf().disable()
            .securityContextRepository(securityContextRepository)
            .exceptionHandling()
            .authenticationEntryPoint { exchange, _ ->
                exchange.response.statusCode = HttpStatus.NOT_FOUND
                Mono.empty()
            }
            .accessDeniedHandler { exchange, _ ->
                exchange.response.statusCode = HttpStatus.NOT_FOUND
                Mono.empty()
            }
            .and()
            .authorizeExchange()
            .pathMatchers("/api/**", "/rsocket/**").permitAll()
            .pathMatchers("/", "/readme", "/readme/**").permitAll()
            .pathMatchers("/swagger").permitAll()
            .anyExchange().authenticated()
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
            .build()

    @Bean
    fun userDetailsService() = ReactiveUserDetailsService { Mono.empty() }
}