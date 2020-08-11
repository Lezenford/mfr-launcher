package ru.fullrest.mfr.plugins_configuration_utility.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey

interface PropertiesRepository : JpaRepository<Properties, Int> {
    fun findByKey(key: PropertyKey): Properties?

    //    @Query(
//        """
//            select case when (count (p) > 0) then true else false end from Properties p
//            where p.key = :key
//        """
//    )
    fun existsByKey(key: PropertyKey): Boolean
}