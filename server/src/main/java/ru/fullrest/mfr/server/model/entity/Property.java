package ru.fullrest.mfr.server.model.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "ServerProperty")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PropertyId")
    private int id;

    @Column(name = "Type", unique = true)
    @Enumerated(EnumType.STRING)
    private PropertyType type;

    @Column(name = "Value")
    private String value;
}
