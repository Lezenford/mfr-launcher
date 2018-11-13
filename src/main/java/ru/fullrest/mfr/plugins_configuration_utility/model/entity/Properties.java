package ru.fullrest.mfr.plugins_configuration_utility.model.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@Data
@Entity
@Table(name = "PROPERTY")
public class Properties {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "KEY")
    @Enumerated(EnumType.STRING)
    private PropertyKey key;

    @Column(name = "VALUE")
    private String value;
}
