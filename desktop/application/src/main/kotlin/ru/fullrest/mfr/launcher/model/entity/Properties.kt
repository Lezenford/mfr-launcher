package ru.fullrest.mfr.launcher.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

@Entity
@Table(name = "property")
class Properties(

    @Column(name = "key")
    @Enumerated(EnumType.STRING)
    val key: Key,

    @Column(name = "value")
    var value: String? = null

) : BaseEntity() {
    enum class Key {
        SCHEMA, FIRST_START, SELECTED_BUILD, LAST_UPDATE_DATE
    }
}