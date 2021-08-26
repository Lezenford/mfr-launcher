package ru.fullrest.mfr.launcher.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.launcher.model.entity.Option

interface OptionRepository : JpaRepository<Option, Int> {
}