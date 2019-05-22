package ru.fullrest.mfr.plugins_configuration_utility.model.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RepositoryService {

    private final GroupRepository groupRepository;

    public List<Group> getAllGroupsWithLazyInit() {
        List<Group> groups = new ArrayList<>();
        groupRepository.findAll().forEach(groups::add);
        groups.forEach(group -> group.getReleases().forEach(release -> release.getDetails().size()));
        return groups;
    }
}