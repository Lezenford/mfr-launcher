package ru.fullrest.mfr.plugins_configuration_utility.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release

interface ReleaseRepository : JpaRepository<Release, Int> {
    fun findAllByGroup(group: Group): List<Release>

    fun findFirstByGroupAndAppliedIsTrue(group: Group): Release

    fun findAllByAppliedIsTrue(): List<Release>

    @Query("UPDATE Release r SET r.applied = :apply WHERE r = :release")
    fun updateIsApliedByRelise(release: Release, apply: Boolean)
}