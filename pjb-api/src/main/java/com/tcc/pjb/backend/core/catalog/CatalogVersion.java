package com.tcc.pjb.backend.core.catalog;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pjb_catalog_version", indexes = {
        @Index(name = "idx_cat_key_active", columnList = "catalog_key, active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_cat_key_version", columnNames = {"catalog_key", "version"})
})
public class CatalogVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalog_key", nullable = false, length = 80)
    private String key;

    @Column(name = "version", nullable = false, length = 40)
    private String version;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
