package ru.fullrest.mfr.plugins_configuration_utility.model.entity

import javax.persistence.*

@Entity
@Table(name = "PROPERTY")
class Properties(

    @Column(name = "KEY", unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    val key: PropertyKey,

    @Column(name = "VALUE")
    var value: String? = null
) : BaseEntity()