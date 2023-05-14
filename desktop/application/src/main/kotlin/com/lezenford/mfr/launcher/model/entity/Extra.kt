package com.lezenford.mfr.launcher.model.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "extra")
class Extra(
    @Column(name = "name")
    val name: String,

    @Column(name = "downloaded")
    var downloaded: Boolean = false,

    @OneToMany(mappedBy = "extra", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    val files: MutableList<ExtraFile> = mutableListOf()
) : BaseEntity()

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