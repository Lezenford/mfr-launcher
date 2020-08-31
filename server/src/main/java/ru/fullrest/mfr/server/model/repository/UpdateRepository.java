package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.Update;

import java.util.List;

public interface UpdateRepository extends CrudRepository<Update, Integer> {

    @Query(
            "select u.version from Update u " +
                    "where u.active = true " +
                    "order by u.id"
    )
    List<String> findAllActive();

    Update findByVersion(String version);
}
