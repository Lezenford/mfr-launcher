package ru.fullrest.mfr.server.model.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.server.model.entity.User;
import ru.fullrest.mfr.server.model.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            return org.springframework.security.core.userdetails.User.builder()
                                                                     .username(user.getUsername())
                                                                     .password(user.getPassword())
                                                                     .roles(user.getRole().toString())
                                                                     .build();
        }
        return null;
    }
}
