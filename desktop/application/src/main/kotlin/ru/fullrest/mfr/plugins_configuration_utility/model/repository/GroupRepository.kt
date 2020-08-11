package ru.fullrest.mfr.plugins_configuration_utility.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group

interface GroupRepository : JpaRepository<Group, Int> {
    fun findAllByOrderByValue(): List<Group>

    @Query(
        """SELECT g FROM Group g 
JOIN FETCH g.releases 
ORDER BY g.value"""
    )
    fun findAllWithReleases(): List<Group>
}