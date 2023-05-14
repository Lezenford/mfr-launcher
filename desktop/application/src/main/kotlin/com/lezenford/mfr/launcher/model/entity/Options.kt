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
@Table(name = "sections")
class Section(

    @Column(name = "name")
    var name: String,


    @Column(name = "downloaded")
    var downloaded: Boolean,

    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "section",
        cascade = [CascadeType.ALL]
    )
    val options: MutableList<Option> = mutableListOf()
) : BaseEntity()


@Entity
@Table(name = "options")
class Option(

    @Column(name = "name")
    var name: String,

    @Column(name = "applied")
    var applied: Boolean,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    val section: Section,

    @Column(name = "image_path")
    var image: String? = null,

    @Column(name = "description")
    var description: String = "",

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "option", cascade = [CascadeType.ALL], orphanRemoval = true)
    val files: MutableList<OptionFile> = mutableListOf()
) : BaseEntity() {
    override fun toString(): String = name
}

@Entity
@Table(name = "items")
class OptionFile(

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
