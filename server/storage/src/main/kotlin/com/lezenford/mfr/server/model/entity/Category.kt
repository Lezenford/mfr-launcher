package com.lezenford.mfr.server.model.entity

import com.lezenford.mfr.common.protocol.enums.ContentType
import javax.persistence.*

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "BuildId")
    val build: Build
) : Base<Int>()