package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.configuration.CacheConfiguration
import com.lezenford.mfr.server.model.entity.Category
import com.lezenford.mfr.server.model.repository.CategoryRepository
import org.hibernate.Hibernate
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {
    @Transactional
    @Cacheable(value = [CacheConfiguration.CATEGORY_CACHE], unless = "#result.isEmpty()")
    fun findAllByBuildId(buildId: Int): List<Category> {
        return categoryRepository.findAllByBuildId(buildId).onEach { category ->
            Hibernate.initialize(category.items)
            category.items.forEach { Hibernate.initialize(it.files) }
        }
    }

    @Transactional
    @CacheEvict(value = [CacheConfiguration.CATEGORY_CACHE, CacheConfiguration.FILE_CACHE], allEntries = true)
    fun saveAll(categories: Collection<Category>) {
        categoryRepository.saveAll(categories)
    }
}