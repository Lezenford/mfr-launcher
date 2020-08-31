package ru.fullrest.mfr.server.model.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "GameDownloadHistory")
public class GameDownloadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryId")
    private int id;

    @Column(name = "Key")
    private String clientKey;
}
