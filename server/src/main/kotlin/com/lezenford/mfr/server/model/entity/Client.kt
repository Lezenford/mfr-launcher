package com.lezenford.mfr.server.model.entity

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "Client")
class Client(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    override val id: Int = 0,

    @Column(name = "UUID")
    val uuid: String = UUID.randomUUID().toString(),

    @Column(name = "LastConnection")
    val lastConnect: LocalDateTime = LocalDateTime.now()
) : Base<Int>()