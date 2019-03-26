package ru.fullrest.mfr.server.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public abstract class FilePathEntity extends BaseEntity {

    @Column(name = "PATH", unique = true)
    private String path;
}
