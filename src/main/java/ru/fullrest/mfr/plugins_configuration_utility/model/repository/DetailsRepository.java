package ru.fullrest.mfr.plugins_configuration_utility.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release;

import java.util.List;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
public interface DetailsRepository extends CrudRepository<Details, Integer> {
    List<Details> findAllByRelease(Release release);
}
