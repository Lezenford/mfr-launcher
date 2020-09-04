package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.dto.StatisticsCountDto;
import ru.fullrest.mfr.server.model.entity.UpdateDownloadHistory;

import java.util.List;

public interface UpdateDownloadHistoryRepository extends CrudRepository<UpdateDownloadHistory, Integer> {
    Boolean existsByClientKeyAndVersion(String clientKey, String version);

    @Query(
            "select new ru.fullrest.mfr.server.model.dto.StatisticsCountDto( g.version, count( g.clientKey ) ) " +
                    "from UpdateDownloadHistory g " +
                    "group by g.version " +
                    "order by g.version"
    )
    List<StatisticsCountDto> countAll();
}
