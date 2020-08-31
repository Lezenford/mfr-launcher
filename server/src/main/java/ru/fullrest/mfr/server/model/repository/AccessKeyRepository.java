package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.AccessKey;

import java.util.Optional;

public interface AccessKeyRepository extends CrudRepository<AccessKey, Long> {
    Optional<AccessKey> findByKey(String key);
}
