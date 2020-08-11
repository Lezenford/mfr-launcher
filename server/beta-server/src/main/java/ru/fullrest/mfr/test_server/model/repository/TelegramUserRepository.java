package ru.fullrest.mfr.test_server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.test_server.model.entity.TelegramUser;

public interface TelegramUserRepository extends CrudRepository<TelegramUser, Integer> {
}
