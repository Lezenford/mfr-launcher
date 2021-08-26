package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Launcher
import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.common.api.SystemType

interface LauncherRepository : JpaRepository<Launcher, Int> {

    fun findBySystem(system: SystemType): Launcher?
}