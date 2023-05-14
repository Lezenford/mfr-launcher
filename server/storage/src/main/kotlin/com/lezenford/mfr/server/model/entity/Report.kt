package com.lezenford.mfr.server.model.entity

import com.lezenford.mfr.common.protocol.enums.ReportType
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.*

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