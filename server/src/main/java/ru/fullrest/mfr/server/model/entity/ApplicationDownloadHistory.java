package ru.fullrest.mfr.server.model.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "ApplicationDownloadHistory")
public class ApplicationDownloadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryId")
    private int id;

    @Column(name = "Key")
    private String clientKey;

    @Column(name = "Version")
    private String version;
}
