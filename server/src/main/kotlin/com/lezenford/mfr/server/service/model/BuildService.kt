package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.model.repository.BuildRepository
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class BuildService(
    private val buildRepository: BuildRepository
) {

    @Transactional
    fun save(build: Build) = buildRepository.save(build)

    fun findByName(name: String): Build? = buildRepository.findByName(name)

    fun findById(id: Int): Build? = buildRepository.findById(id).orElse(null)

    fun findAll(): List<Build> = buildRepository.findAll()

    @Transactional
    fun updateDefault(id: Int) {
        buildRepository.resetDefault()
        buildRepository.setDefault(id)
    }
}