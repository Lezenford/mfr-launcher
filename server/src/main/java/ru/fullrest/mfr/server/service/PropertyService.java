package ru.fullrest.mfr.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.server.configuration.CacheConfiguration;
import ru.fullrest.mfr.server.model.entity.Property;
import ru.fullrest.mfr.server.model.entity.PropertyType;
import ru.fullrest.mfr.server.model.repository.PropertyRepository;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;

    @Cacheable(value = CacheConfiguration.PROPERTY_CACHE, unless = "#result == null")
    public Property findByType(PropertyType type) {
        return propertyRepository.findByType(type).orElse(null);
    }

    @Transactional
    @CacheEvict(value = CacheConfiguration.PROPERTY_CACHE, allEntries = true)
    public void save(Property property) {
        propertyRepository.save(property);
    }
}
