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
@Table(name = "RELEASE")
public class Release {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "VALUE")
    private String value;

    @Column(name = "DEFAULT")
    private boolean defaultRelease;

    @Column(name = "APPLIED")
    private boolean applied;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PLUGIN_GROUP_ID")
    private Group group;

    @Column(name = "IMAGE_PATH")
    private String image;

    @Column(name = "ACTIVE")
    private boolean active;

    @Column(name = "DESCRIPTION")
    private String description;

    @Override
    public String toString() {
        return value;
    }
}
