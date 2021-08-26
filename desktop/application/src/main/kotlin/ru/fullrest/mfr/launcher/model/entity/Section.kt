package ru.fullrest.mfr.launcher.model.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
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