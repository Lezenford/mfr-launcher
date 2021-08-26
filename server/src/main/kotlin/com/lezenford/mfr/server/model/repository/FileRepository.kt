package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.File
import org.springframework.data.jpa.repository.JpaRepository

interface FileRepository : JpaRepository<File, Int> {
}