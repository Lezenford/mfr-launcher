package ru.fullrest.mfr.server.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "UPDATES", uniqueConstraints = @UniqueConstraint(columnNames = {"VERSION", "PLATFORM"}))
@Data
public class Update extends FilePathEntity {

    @Column(name = "VERSION", nullable = false)
    private String version;

    @Column(name = "PLATFORM", nullable = false)
    private String platform;

    @Column(name = "UPLOAD_DATE", nullable = false)
    private Date uploadDate = new Date();

    @Column(name = "SCHEMA_UPDATE")
    @ColumnDefault("FALSE")
    private boolean schemaUpdate = false;

    @Column(name = "APPLIED_UPDATE")
    @ColumnDefault("FALSE")
    private boolean appliedUpdate = false;

    @Column(name = "ACTIVE")
    @ColumnDefault("TRUE")
    private boolean active = true;
}
