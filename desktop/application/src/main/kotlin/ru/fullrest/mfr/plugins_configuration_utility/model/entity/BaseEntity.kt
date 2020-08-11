package ru.fullrest.mfr.plugins_configuration_utility.model.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@MappedSuperclass
abstract class BaseEntity(
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Int = 0
) {
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseEntity

        if (id != other.id) return false

        return true
    }

    final override fun hashCode(): Int {
        return id
    }
}