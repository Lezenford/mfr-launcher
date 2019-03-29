package ru.fullrest.mfr.plugins_configuration_utility.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "RELEASE", indexes = {
        @Index(name = "RELEASE_UNIQUE_INDEX", columnList = "PLUGIN_GROUP_ID, VALUE", unique = true)
})
public class Release extends BaseEntity {

    @Column(name = "VALUE", nullable = false)
    private String value;

    @Column(name = "APPLIED", columnDefinition = "BOOLEAN DEFAULT TRUE", nullable = false)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private boolean applied = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PLUGIN_GROUP_ID", nullable = false)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Group group;

    @Column(name = "IMAGE_PATH")
    @EqualsAndHashCode.Exclude
    private String image;

    @Column(name = "DESCRIPTION")
    @EqualsAndHashCode.Exclude
    private String description;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "release", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private List<Details> details;

    @Override
    public String toString() {
        return getValue();
    }
}