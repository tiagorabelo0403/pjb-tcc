package com.tcc.pjb.backend.model.entity.digitalizacao;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pjb_digitalizacao_pagina", indexes = {
        @Index(name = "idx_digitalizacao_pagina_job", columnList = "job_id"),
        @Index(name = "idx_digitalizacao_pagina_tipo", columnList = "tipo_peca")
})
@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalizacaoPagina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "numero_pagina", nullable = false)
    private Integer numeroPagina;

    @Column(name = "conteudo_ocr", columnDefinition = "TEXT")
    private String conteudoOcr;

    @Column(name = "confianca", precision = 5, scale = 2)
    private BigDecimal confianca;

    @Column(name = "tipo_peca", length = 64)
    private String tipoPeca;

    @Column(name = "revisado", nullable = false)
    private boolean revisado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
