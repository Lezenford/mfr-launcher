package com.lezenford.mfr.launcher.model.repository

import com.lezenford.mfr.launcher.model.entity.Extra
import org.springframework.data.jpa.repository.JpaRepository

interface ExtraRepository : JpaRepository<Extra, Int>