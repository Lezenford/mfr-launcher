package com.lezenford.mfr.server.configuration

import com.lezenford.mfr.server.security.TelegramUserAuthenticationProvider
import com.lezenford.mfr.server.security.UuidFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class SecurityConfiguration(
    private val uuidFilter: UuidFilter,
    private val authenticationProvider: TelegramUserAuthenticationProvider
) : WebSecurityConfigurerAdapter(), WebMvcConfigurer {

    override fun configure(http: HttpSecurity) {
        http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors()
            .and()
            .csrf().disable()
            .httpBasic().disable()
            .logout().disable()
            .formLogin().disable()
            .authorizeRequests()
            .anyRequest().permitAll()
            .and()
            .addFilterBefore(uuidFilter, BasicAuthenticationFilter::class.java)
            .authenticationProvider(authenticationProvider)
    }


    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedHeaders("*")
            .allowedOrigins("*")
            .allowedMethods(*HttpMethod.values().map { it.name }.toTypedArray())
    }

    @Bean
    override fun authenticationManager(): AuthenticationManager =
        super.authenticationManager().run {
            AuthenticationManager { authentication ->
                this.authenticate(authentication).also {
                    SecurityContextHolder.getContext().authentication = it
                }
            }
        }
}