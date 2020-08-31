package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.GameDownloadHistory;

public interface GameDownloadHistoryRepository extends CrudRepository<GameDownloadHistory, Integer> {

    Boolean existsByClientKey(String clientKey);

    @Query("select count(g) from GameDownloadHistory g")
    Integer countAll();
}
