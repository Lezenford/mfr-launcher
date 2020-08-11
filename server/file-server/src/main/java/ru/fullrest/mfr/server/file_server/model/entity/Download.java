package ru.fullrest.mfr.server.file_server.model.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "DOWNLOAD")
public class Download {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private int id;

    @Column(name = "PATH")
    private String path;
}
