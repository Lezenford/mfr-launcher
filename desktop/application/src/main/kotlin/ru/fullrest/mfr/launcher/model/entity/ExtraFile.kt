package ru.fullrest.mfr.launcher.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "extra_files")
class ExtraFile(
    @Column(name = "path")
    val path: String,

    @Column(name = "md5")
    private val md5: ByteArray,

    @ManyToOne
    @JoinColumn(name = "extra_id")
    val extra: Extra
) : BaseEntity()