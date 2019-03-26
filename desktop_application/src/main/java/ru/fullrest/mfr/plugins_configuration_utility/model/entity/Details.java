package ru.fullrest.mfr.plugins_configuration_utility.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "DETAILS", indexes = {
        @Index(name = "DETAILS_UNIQUE_INDEX", columnList = "RELEASE_ID, STORAGE_PATH", unique = true)
})
public class Details extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RELEASE_ID", nullable = false)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Release release;

    @Column(name = "STORAGE_PATH", nullable = false)
    private String storagePath;

    @Column(name = "GAME_PATH", nullable = false)
    private String gamePath;

    @Column(name = "MD5")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private byte[] md5;
}
