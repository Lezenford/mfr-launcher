package ru.fullrest.mfr.server.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "APPLICATIONS")
@Data
public class Application extends FilePathEntity {

    @Column(name = "OPERATION_SYSTEM", nullable = false, unique = true)
    private String operationSystem;

    @Column(name = "SHORT_NAME", nullable = false, unique = true)
    private String shortName;

    @Column(name = "VERSION", nullable = false)
    private String version;
}