package com.lezenford.mfr.launcher.service.model

import com.lezenford.mfr.launcher.model.entity.Option
import com.lezenford.mfr.launcher.model.repository.OptionRepository
import org.springframework.stereotype.Service
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