package ru.fullrest.mfr.launcher.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.launcher.model.entity.Section

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