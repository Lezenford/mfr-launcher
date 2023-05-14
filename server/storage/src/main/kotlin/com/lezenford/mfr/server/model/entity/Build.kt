package com.lezenford.mfr.server.model.entity

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "Build")
class Build(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    override val id: Int = 0,

    @Column(name = "Name")
    val name: String,

    @Column(name = "Branch")
    val branch: String,

    @Column(name = "UseByDefault")
    val default: Boolean = false,

    @Column(name = "lastUpdateDate")
    var lastUpdateDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
) : Base<Int>()