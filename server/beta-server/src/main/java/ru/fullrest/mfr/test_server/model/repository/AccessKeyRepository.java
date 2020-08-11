package ru.fullrest.mfr.test_server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.test_server.model.entity.AccessKey;

import java.util.Optional;

public interface AccessKeyRepository extends CrudRepository<AccessKey, Long> {
    Optional<AccessKey> findByKey(String key);
}
