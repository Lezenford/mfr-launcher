package ru.fullrest.mfr.test_server.model.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "TelegramUser")
public class TelegramUser {

    @Id
    @Column(name = "UserId")
    private int id;

    @Column(name = "Username")
    private String username;

    @Column(name = "Role")
    @Enumerated(EnumType.STRING)
    private UserRole role;
}
