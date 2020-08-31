package ru.fullrest.mfr.server.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.server.model.entity.Property;
import ru.fullrest.mfr.server.model.entity.PropertyType;

import java.util.Optional;

public interface PropertyRepository extends CrudRepository<Property, Integer> {

    Optional<Property> findByType(PropertyType type);
}
