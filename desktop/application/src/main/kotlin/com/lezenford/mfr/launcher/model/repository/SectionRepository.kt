package com.lezenford.mfr.launcher.model.repository

import com.lezenford.mfr.launcher.model.entity.Section
import org.springframework.data.jpa.repository.JpaRepository

interface SectionRepository : JpaRepository<Section, Int> {
//    fun findAllByOrderByValue(): List<Group>
//
//    @Query(
//        """SELECT g FROM Group g
//JOIN FETCH g.options
//ORDER BY g.name"""
//    )
//    fun findAllWithReleases(): List<Group>
}