package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.server.model.entity.Launcher
import org.springframework.data.jpa.repository.JpaRepository

interface LauncherRepository : JpaRepository<Launcher, Int> {

    fun findBySystem(system: SystemType): Launcher?
}