package ru.fullrest.mfr.plugins_configuration_utility.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "PROPERTY")
public class Properties extends BaseEntity {

    @Column(name = "KEY", unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    private PropertyKey key;

    @Column(name = "VALUE")
    private String value;
}