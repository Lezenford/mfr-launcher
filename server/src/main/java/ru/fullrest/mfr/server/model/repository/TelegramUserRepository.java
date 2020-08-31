package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.TelegramUser;

public interface TelegramUserRepository extends CrudRepository<TelegramUser, Integer> {
}
