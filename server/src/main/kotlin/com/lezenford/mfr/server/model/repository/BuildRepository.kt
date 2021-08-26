package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Build
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface BuildRepository : JpaRepository<Build, Int> {
    fun findByName(name: String): Build?

    @Modifying
    @Query(
        """
            update Build b set b.default = false
        """
    )
    fun resetDefault()

    @Modifying
    @Query(
        """
            update Build b set b.default = true where b.id = :id
        """
    )
    fun setDefault(id: Int)
}