package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.dto.StatisticsCountDto;
import ru.fullrest.mfr.server.model.entity.ApplicationDownloadHistory;

import java.util.List;

public interface ApplicationDownloadHistoryRepository extends CrudRepository<ApplicationDownloadHistory, Integer> {
    Boolean existsByClientKeyAndVersion(String clientKey, String version);

    @Query(
            "select new ru.fullrest.mfr.server.model.dto.StatisticsCountDto( g.version, count( g.clientKey ) ) " +
                    "from ApplicationDownloadHistory g " +
                    "group by g.version " +
                    "order by g.version"
    )
    List<StatisticsCountDto> countAll();
}
