package com.lezenford.mfr.server.model.entity

import ru.fullrest.mfr.common.api.ReportType
import java.time.LocalDateTime
import java.time.ZoneOffset
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
import javax.persistence.Table

@Entity
@Table(name = "Report")
class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    override val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ClientId")
    val client: Client,

    @Column(name = "Type")
    @Enumerated(EnumType.STRING)
    val type: ReportType,

    @Column(name = "Text")
    val text: String,

    @Column(name = "UploadDateTime")
    val uploadDateTime: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),

    @Column(name = "Forwarded")
    var forwarded: Boolean = false
) : Base<Long>()