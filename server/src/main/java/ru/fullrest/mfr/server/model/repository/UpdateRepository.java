package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.Update;

import java.util.List;

public interface UpdateRepository extends CrudRepository<Update, Integer> {
    Update findFirstByPlatformAndActiveIsTrueOrderByIdDesc(String platform);

    Update findByVersionAndPlatform(String version, String platform);

    List<Update> findByPlatformAndActiveIsTrueAndIdIsGreaterThanOrderByIdAsc(String platform, int id);

    List<Update> findAllByPlatformOrderByIdAsc(String platform);

    List<Update> findAllByOrderByIdAsc();
}