package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Property
import org.springframework.data.jpa.repository.JpaRepository

interface PropertyRepository : JpaRepository<Property, Int> {
    fun findByType(type: Property.Type): Property?
}