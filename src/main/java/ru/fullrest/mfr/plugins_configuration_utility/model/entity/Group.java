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
@Table(name = "PLUGIN_GROUP")
public class Group {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "VALUE")
    private String value;

    @Column(name = "ACTIVE")
    private boolean active;

    @Override
    public String toString() {
        return value;
    }
}
