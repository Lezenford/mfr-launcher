package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.User;

public interface UserRepository extends CrudRepository<User, Integer> {
    User findByUsername(String username);
}
