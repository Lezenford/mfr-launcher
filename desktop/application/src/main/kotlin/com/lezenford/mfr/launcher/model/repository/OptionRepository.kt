package com.lezenford.mfr.launcher.model.repository

import com.lezenford.mfr.launcher.model.entity.Option
import org.springframework.data.jpa.repository.JpaRepository

interface OptionRepository : JpaRepository<Option, Int> {
    //    fun findAllByGroup(group: Group): List<Option>
//
//    fun findFirstByGroupAndAppliedIsTrue(group: Group): Option
//
//    fun findAllByAppliedIsTrue(): List<Option>
//
//    @Query("UPDATE Option r SET r.applied = :apply WHERE r = :release")
//    fun updateIsApliedByRelise(release: Option, apply: Boolean)
}