package ru.fullrest.mfr.launcher.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.launcher.model.entity.Extra

interface ExtraRepository : JpaRepository<Extra, Int>