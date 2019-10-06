package ru.fullrest.mfr.server;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.fullrest.mfr.server.config.PropertiesConfiguration;
import ru.fullrest.mfr.server.config.SecurityConfiguration;
import ru.fullrest.mfr.server.model.entity.Role;
import ru.fullrest.mfr.server.model.entity.User;
import ru.fullrest.mfr.server.model.repository.UserRepository;

@PropertySource(value = "file:external.properties")
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class ServerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    private final SecurityConfiguration securityConfiguration;

    private final PropertiesConfiguration propertiesConfiguration;

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        Iterable<User> users = userRepository.findAll();
        if (!users.iterator().hasNext()) {
            User user = new User();
            user.setUsername(propertiesConfiguration.getDefaultLogin());
            user.setPassword(securityConfiguration.passwordEncoder()
                                                  .encode(propertiesConfiguration.getDefaultPassword()));
            user.setRole(Role.ADMIN);
            userRepository.save(user);
        }
    }
}
