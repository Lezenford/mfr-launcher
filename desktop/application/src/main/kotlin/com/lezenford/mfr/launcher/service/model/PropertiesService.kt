package com.lezenford.mfr.launcher.service.model

import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.model.repository.PropertiesRepository
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class PropertiesService(
    private val repository: PropertiesRepository
) {

    fun findByKey(key: Properties.Key) = repository.findByKey(key)

    @Transactional
    fun save(property: Properties) {
        repository.save(property)
    }

    @Transactional
    fun updateValue(key: Properties.Key, value: String? = null) {
        val properties = repository.findByKey(key)?.also {
            it.value = value
        } ?: Properties(key, value)
        repository.save(properties)
    }
}