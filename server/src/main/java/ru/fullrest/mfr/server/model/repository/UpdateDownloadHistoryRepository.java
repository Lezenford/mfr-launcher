package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.UpdateDownloadHistory;

public interface UpdateDownloadHistoryRepository extends CrudRepository<UpdateDownloadHistory, Integer> {
    Boolean existsByClientKey(String clientKey);

    @Query("select count(g) from UpdateDownloadHistory g")
    Integer countAll();
}
