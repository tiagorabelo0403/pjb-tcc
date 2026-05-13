package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "metadados_sistema")
public class MetadadosSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Column(name = "chave", nullable = false, unique = true)
    private String chave;

    @Column(name = "valor", nullable = false)
    private String valor;

    private String fonte;
    private String versao;

    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    public void prePersist() {
        this.dataRegistro = LocalDateTime.now();
    }
}