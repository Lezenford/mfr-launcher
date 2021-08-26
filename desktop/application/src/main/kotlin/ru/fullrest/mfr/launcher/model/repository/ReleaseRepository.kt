package ru.fullrest.mfr.launcher.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.launcher.model.entity.Option

interface ReleaseRepository : JpaRepository<Option, Int> {
//    fun findAllByGroup(group: Group): List<Option>
//
//    fun findFirstByGroupAndAppliedIsTrue(group: Group): Option
//
//    fun findAllByAppliedIsTrue(): List<Option>
//
//    @Query("UPDATE Option r SET r.applied = :apply WHERE r = :release")
//    fun updateIsApliedByRelise(release: Option, apply: Boolean)
}