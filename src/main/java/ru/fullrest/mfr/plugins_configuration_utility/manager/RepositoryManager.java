package ru.fullrest.mfr.plugins_configuration_utility.manager;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Log4j2
@Component
@Transactional(readOnly = true)
public class RepositoryManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void initUpdateScript(String script) {
        entityManager.createNativeQuery(script).executeUpdate();
    }
}
