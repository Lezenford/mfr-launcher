package ru.fullrest.mfr.plugins_configuration_utility.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release

interface DetailsRepository : JpaRepository<Details, Int> {
    fun findAllByRelease(release: Release): List<Details>
}