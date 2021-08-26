package ru.fullrest.mfr.launcher.service

import org.springframework.stereotype.Service
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.model.repository.PropertiesRepository
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
    fun updateValue(key: Properties.Key, value: String) {
        val properties = repository.findByKey(key)?.also {
            it.value = value
        } ?: Properties(key, value)
        repository.save(properties)
    }
}