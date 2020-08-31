package ru.fullrest.mfr.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.fullrest.mfr.server.security.SecurityService;

@Profile("private")
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final SecurityService securityService;

    @GetMapping("auth")
    public String auth(@RequestParam String key) {
        return securityService.createToken(key);
    }
}
