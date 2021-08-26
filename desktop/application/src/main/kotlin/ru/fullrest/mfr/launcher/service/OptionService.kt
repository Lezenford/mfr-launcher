package ru.fullrest.mfr.launcher.service

import org.springframework.stereotype.Service
import ru.fullrest.mfr.launcher.model.entity.Option
import ru.fullrest.mfr.launcher.model.repository.OptionRepository
import javax.transaction.Transactional

@Service
class OptionService(
    private val optionRepository: OptionRepository
) {

    @Transactional
    fun save(option: Option) {
        optionRepository.save(option)
    }
}