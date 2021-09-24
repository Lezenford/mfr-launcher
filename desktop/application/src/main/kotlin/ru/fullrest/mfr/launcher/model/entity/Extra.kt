package ru.fullrest.mfr.launcher.model.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
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