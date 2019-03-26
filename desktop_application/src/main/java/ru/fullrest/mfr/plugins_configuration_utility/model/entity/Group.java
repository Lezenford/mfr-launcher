package ru.fullrest.mfr.plugins_configuration_utility.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "PLUGIN_GROUP")
public class Group extends BaseEntity {

    @Column(name = "VALUE", nullable = false, unique = true)
    private String value;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private List<Release> releases;

    @Override
    public String toString() {
        return getValue();
    }
}
