package com.lezenford.mfr.server.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "ServerProperty")
class Property(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PropertyId")
    override val id: Int = 0,

    @Column(name = "Type")
    @Enumerated(EnumType.STRING)
    val type: Type,

    @Column(name = "Value")
    val value: String,
) : Base<Int>() {
    enum class Type {
        GAME_ARCHIVE,
        LAUNCHER,
        LAUNCHER_UPDATER,
        LAUNCHER_VERSION
    }
}

