package com.lezenford.mfr.server.model.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "History")
class History(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    override val id: Int = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ItemId")
    val item: Item,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ClientId")
    val client: Client,

    @Column(name = "LastChangeDate")
    var lastChangeDate: LocalDateTime = LocalDateTime.now()
) : Base<Int>()