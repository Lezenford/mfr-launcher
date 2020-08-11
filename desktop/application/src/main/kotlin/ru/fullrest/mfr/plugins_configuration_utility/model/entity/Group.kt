package ru.fullrest.mfr.plugins_configuration_utility.model.entity

import javax.persistence.*

@Entity
@Table(name = "PLUGIN_GROUP")
class Group(

    @Column(name = "VALUE", nullable = false, unique = true)
    var value: String,

    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "group",
        cascade = [CascadeType.ALL]
    )
    val releases: MutableList<Release> = mutableListOf()
) : BaseEntity()