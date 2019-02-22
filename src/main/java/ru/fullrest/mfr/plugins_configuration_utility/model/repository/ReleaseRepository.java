package ru.fullrest.mfr.plugins_configuration_utility.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release;

import java.util.List;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
public interface ReleaseRepository extends CrudRepository<Release, Integer> {
    List<Release> findAllByGroup(Group group);

    List<Release> findAllByGroupAndAppliedIsTrue(Group group);
}
