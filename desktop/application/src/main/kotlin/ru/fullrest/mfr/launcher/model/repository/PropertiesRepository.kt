package ru.fullrest.mfr.launcher.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.launcher.model.entity.Properties

interface PropertiesRepository : JpaRepository<Properties, Int> {
    fun findByKey(key: Properties.Key): Properties?
//
//    //    @Query(
////        """
////            select case when (count (p) > 0) then true else false end from Properties p
////            where p.key = :key
////        """
////    )
//    fun existsByKey(key: PropertyKey): Boolean
}