package ru.fullrest.mfr.plugins_configuration_utility.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "RELEASE")
class Release(

    @Column(name = "VALUE", nullable = false)
    var value: String,

    @JsonIgnore
    @Column(name = "APPLIED", columnDefinition = "BOOLEAN DEFAULT TRUE", nullable = false)
    var applied: Boolean,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PLUGIN_GROUP_ID", nullable = false)
    var group: Group?,

    @Column(name = "IMAGE_PATH")
    var image: String? = null,

    @Column(name = "DESCRIPTION")
    var description: String = "",

    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "release",
        cascade = [CascadeType.ALL]
    )
    val details: MutableList<Details> = mutableListOf()
) : BaseEntity() {
    override fun toString(): String = value
}