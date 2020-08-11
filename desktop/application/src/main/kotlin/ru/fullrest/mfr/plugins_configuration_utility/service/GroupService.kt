package ru.fullrest.mfr.plugins_configuration_utility.service

import org.hibernate.Hibernate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository

@Service
class GroupService(
    private val groupRepository: GroupRepository
) {

    @Transactional
    fun getAll(): List<Group> =
        groupRepository.findAll().onEach { Hibernate.initialize(it.releases) }

    @Transactional
    fun getAllWithDetails(): List<Group> =
        groupRepository.findAll().onEach { group ->
            Hibernate.initialize(group.releases)
            group.releases.forEach { release ->
                Hibernate.initialize(release.details)
            }
        }

    fun saveAll(groups: List<Group>) {
        groupRepository.saveAll(groups)
    }

    fun removeAll() {
        groupRepository.deleteAll()
    }

    fun removeAll(groups: List<Group>) {
        groupRepository.deleteAll(groups)
    }
}