package com.lezenford.mfr.server.model.entity

import com.lezenford.mfr.common.protocol.enums.SystemType
import javax.persistence.*

@Entity
@Table(name = "Launcher")
class Launcher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    override val id: Int = 0,

    @Column(name = "OperationSystem")
    @Enumerated(EnumType.STRING)
    val system: SystemType,

    @Column(name = "Version")
    var version: String,

    @Column(name = "MD5")
    var md5: ByteArray = ByteArray(0),

    @Column(name = "FileName")
    var fileName: String,

    @Column(name = "Size")
    var size: Long = 0
) : Base<Int>()