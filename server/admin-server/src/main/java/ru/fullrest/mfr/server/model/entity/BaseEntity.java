package ru.fullrest.mfr.server.model.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
}
