package ru.fullrest.mfr.launcher.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.fullrest.mfr.launcher.model.entity.Item

interface DetailsRepository : JpaRepository<Item, Int> {
//    fun findAllByRelease(release: Option): List<Item>
}