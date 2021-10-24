package ru.fullrest.mfr.launcher.model.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

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
    val items: MutableList<Item> = mutableListOf()
) : BaseEntity() {
    override fun toString(): String = name
}