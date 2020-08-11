package ru.fullrest.mfr.test_server.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AccessKey")
public class AccessKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccessKeyId")
    private long id;

    @Column(name = "Username")
    private String user;

    @Column(name = "Key", nullable = false, unique = true)
    private String key;

    @Column(name = "Used")
    private boolean used;

    @Column(name = "Active")
    private boolean active;

    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "CreateUser")
    private int createdTelegramUser;
}
