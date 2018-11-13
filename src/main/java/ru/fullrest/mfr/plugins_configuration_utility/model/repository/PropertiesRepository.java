package ru.fullrest.mfr.plugins_configuration_utility.model.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
public interface PropertiesRepository extends CrudRepository<Properties, Integer> {

    //    @Query("SELECT p.value FROM Properties p WHERE p.key=?1")
    Properties findByKey(PropertyKey key);
}
