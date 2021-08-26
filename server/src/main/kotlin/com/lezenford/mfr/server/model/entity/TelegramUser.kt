package com.lezenford.mfr.server.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "TelegramUser")
class TelegramUser(
    @Id
    @Column(name = "Id")
    override val id: Long,

    @Column(name = "Username")
    var username: String,

    @Column(name = "Role")
    @Enumerated(EnumType.STRING)
    var role: Role
) : Base<Long>() {
    enum class Role {
        USER, ADMIN
    }
}

