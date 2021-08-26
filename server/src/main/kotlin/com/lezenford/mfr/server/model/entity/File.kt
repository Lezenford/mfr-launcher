package com.lezenford.mfr.server.model.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "File")
class File(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    override val id: Int = 0,

    @Column(name = "Path")
    val path: String,

    @Column(name = "Active")
    var active: Boolean = true,

    @Column(name = "MD5")
    var md5: ByteArray = ByteArray(0),

    @Column(name = "LastChangeDate")
    var lastChangeDate: LocalDateTime,

    @Column(name = "Size")
    var size: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ItemId")
    val item: Item
) : Base<Int>()