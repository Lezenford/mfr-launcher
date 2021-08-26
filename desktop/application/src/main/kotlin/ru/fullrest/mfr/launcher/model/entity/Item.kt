package ru.fullrest.mfr.launcher.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "items")
class Item(

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "option_id")
    val option: Option,

    @Column(name = "storage_path")
    var storagePath: String,

    @Column(name = "game_path")
    var gamePath: String,

    @Column(name = "md5")
    var md5: ByteArray
) : BaseEntity()