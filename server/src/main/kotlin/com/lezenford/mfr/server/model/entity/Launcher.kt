package com.lezenford.mfr.server.model.entity

import ru.fullrest.mfr.common.api.SystemType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "Launcher")
class Launcher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    override val id: Int = 0,

    @Column(name = "System")
    @Enumerated(EnumType.STRING)
    val system: SystemType,

    @Column(name = "Version")
    var version: String,

    @Column(name = "MD5")
    var md5: ByteArray,

    @Column(name = "FileName")
    var fileName: String,

    @Column(name = "Size")
    var size: Long
) : Base<Int>()