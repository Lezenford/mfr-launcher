package ru.fullrest.mfr.plugins_configuration_utility.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "DETAILS")
class Details(

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RELEASE_ID", nullable = false)
    var release: Release?,

    @Column(name = "STORAGE_PATH", nullable = false)
    var storagePath: String,

    @Column(name = "GAME_PATH", nullable = false)
    var gamePath: String
) : BaseEntity()