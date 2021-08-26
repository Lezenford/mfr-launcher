package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Int> {

    fun findAllByBuildId(buildId: Int): List<Category>
}