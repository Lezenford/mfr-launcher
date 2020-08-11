package ru.fullrest.mfr.test_server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private final static String AUTH_PREFIX = "Bearer ";
    private final SecurityService securityService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = parseJwt(request);
        if (token != null) {
            String principal = securityService.getTokenSubject(token);
            if (principal != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                Collections.singleton(new SimpleGrantedAuthority("ROLE_TESTER"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(AUTH_PREFIX)) {
            return header.substring(AUTH_PREFIX.length());
        }
        return null;
    }
}
