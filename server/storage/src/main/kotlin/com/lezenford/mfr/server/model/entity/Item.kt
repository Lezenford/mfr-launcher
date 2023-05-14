package com.lezenford.mfr.server.model.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "Item")
class Item(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    override val id: Int = 0,

    @Column(name = "Name")
    val name: String,

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val files: MutableList<File> = mutableListOf(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CategoryId")
    val category: Category,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "item")
    val history: List<History> = mutableListOf()
) : Base<Int>()