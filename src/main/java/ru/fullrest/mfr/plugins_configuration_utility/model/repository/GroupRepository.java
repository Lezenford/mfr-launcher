package ru.fullrest.mfr.plugins_configuration_utility.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;

import java.util.List;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
public interface GroupRepository extends CrudRepository<Group, Integer> {
    List<Group> findAllByActiveIsTrue();
}
