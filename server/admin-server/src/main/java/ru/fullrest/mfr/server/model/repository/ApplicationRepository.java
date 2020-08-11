package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.Application;

public interface ApplicationRepository extends CrudRepository<Application, Integer> {
    Application findByShortName(String shortName);
}
