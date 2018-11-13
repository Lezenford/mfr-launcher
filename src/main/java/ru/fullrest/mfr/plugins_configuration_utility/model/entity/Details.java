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
@Table(name = "DETAILS")
public class Details {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RELEASE_ID")
    private Release release;

    @Column(name = "STORAGE_PATH")
    private String storagePath;

    @Column(name = "GAME_PATH")
    private String gamePath;

    @Column(name = "ACTIVE")
    private boolean active;

    @Column(name = "MD5")
    private byte[] md5;
}
