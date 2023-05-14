package com.lezenford.mfr.launcher.model.repository

import com.lezenford.mfr.launcher.model.entity.OptionFile
import org.springframework.data.jpa.repository.JpaRepository

interface DetailsRepository : JpaRepository<OptionFile, Int> {
//    fun findAllByRelease(release: Option): List<Item>
}