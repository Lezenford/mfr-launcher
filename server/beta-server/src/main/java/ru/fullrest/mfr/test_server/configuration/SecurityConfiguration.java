package ru.fullrest.mfr.test_server.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.fullrest.mfr.test_server.model.repository.AccessKeyRepository;
import ru.fullrest.mfr.test_server.security.JwtTokenFilter;
import ru.fullrest.mfr.test_server.security.JwtTokenService;
import ru.fullrest.mfr.test_server.security.SecurityService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final AccessKeyRepository accessKeyRepository;

    @Value("${security.jwt.key}")
    private String secretKey;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Bean
    public JwtTokenService jwtTokenService() {
        return new JwtTokenService(secretKey, issuer);
    }

    @Bean
    public SecurityService securityService(JwtTokenService jwtTokenService) {
        return new SecurityService(jwtTokenService, accessKeyRepository);
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter(SecurityService securityService) {
        return new JwtTokenFilter(securityService);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                .antMatchers("/**").permitAll()
                .antMatchers("/telegram/**").permitAll()
                .antMatchers("/auth").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)).and()
                .httpBasic().disable()
                .logout().disable()
                .formLogin().disable();
        http.addFilterBefore(jwtTokenFilter(securityService(jwtTokenService())), UsernamePasswordAuthenticationFilter.class);
    }
}
