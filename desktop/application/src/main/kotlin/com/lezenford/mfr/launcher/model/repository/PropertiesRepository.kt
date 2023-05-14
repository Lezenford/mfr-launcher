package com.lezenford.mfr.launcher.model.repository

import com.lezenford.mfr.launcher.model.entity.Properties
import org.springframework.data.jpa.repository.JpaRepository

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