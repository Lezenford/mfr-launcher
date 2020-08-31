package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.ApplicationDownloadHistory;

public interface ApplicationDownloadHistoryRepository extends CrudRepository<ApplicationDownloadHistory, Integer> {
    Boolean existsByClientKey(String clientKey);

    @Query("select count(g) from ApplicationDownloadHistory g")
    Integer countAll();
}
