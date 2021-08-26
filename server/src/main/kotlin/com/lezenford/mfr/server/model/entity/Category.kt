package com.lezenford.mfr.server.model.entity

import ru.fullrest.mfr.common.api.ContentType
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "Category")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    override val id: Int = 0,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: ContentType,

    @Column(name = "Required")
    val required: Boolean = false,

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<Item> = mutableListOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuildId")
    val build: Build
) : Base<Int>()