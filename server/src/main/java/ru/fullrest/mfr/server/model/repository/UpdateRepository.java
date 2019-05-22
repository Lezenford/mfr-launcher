package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.fullrest.mfr.server.model.entity.Update;

import javax.transaction.Transactional;
import java.util.List;

public interface UpdateRepository extends CrudRepository<Update, Integer> {
    Update findFirstByPlatformAndActiveIsTrueOrderByIdDesc(String platform);

    Update findByVersionAndPlatform(String version, String platform);

    List<Update> findByPlatformAndActiveIsTrueAndIdIsGreaterThanOrderByIdAsc(String platform, int id);

    List<Update> findAllByPlatformOrderByIdAsc(String platform);

    @Query("SELECT u.platform FROM Update u GROUP BY u.platform ORDER BY u.platform ASC")
    List<?> findAllPlatformByOrderByPlatformAsc();

    @Transactional
    @Modifying
    @Query("UPDATE Update u SET u.downloadCount=u.downloadCount+1 WHERE u.id=:id")
    void incrementDownloadCount(@Param("id") int id);
}