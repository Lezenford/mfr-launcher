package com.lezenford.mfr.manager.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("TelegramUser")
data class TelegramUser(
    @Column("TelegramId")
    val telegramId: Long,

    @Column("Username")
    var username: String,

    @Column("Role")
    var role: Role
) {
    @Id
    @Column("Id")
    private var id: Long = 0

    enum class Role {
        USER, ADMIN
    }
}

