package ru.fullrest.mfr.server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import ru.fullrest.mfr.server.model.entity.AccessKey;
import ru.fullrest.mfr.server.model.repository.AccessKeyRepository;

import java.util.Optional;

@RequiredArgsConstructor
public class SecurityService {
    private final JwtTokenService jwtTokenService;
    private final AccessKeyRepository accessKeyRepository;

    public String createToken(String key) {
        Optional<AccessKey> optional = accessKeyRepository.findByKey(key);
        if (optional.isPresent()) {
            AccessKey accessKey = optional.get();
            if (accessKey.isActive() && !accessKey.isUsed()) {
                String token = jwtTokenService.generateJwtToken(key);
                accessKey.setUsed(true);
                accessKeyRepository.save(accessKey);
                return token;
            }
        }
        throw new BadCredentialsException("Key invalid");
    }

    public String getTokenSubject(String token) {
        String key = jwtTokenService.getSubjectFromJwt(token);
        if (key != null) {
            Optional<AccessKey> optional = accessKeyRepository.findByKey(key);
            if (optional.isPresent()) {
                AccessKey accessKey = optional.get();
                if (accessKey.isActive() && accessKey.isUsed()) {
                    return key;
                }
            }
        }
        return null;
    }
}
