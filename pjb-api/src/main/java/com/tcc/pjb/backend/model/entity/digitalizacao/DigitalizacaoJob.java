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
@Table(name = "pjb_digitalizacao_job", indexes = {
        @Index(name = "idx_digitalizacao_status", columnList = "status,created_at")
})
@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalizacaoJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "numero_processo_origem", length = 64)
    private String numeroProcessoOrigem;

    @Column(name = "sistema_origem", length = 32)
    private String sistemaOrigem;

    @Column(name = "total_paginas")
    private Integer totalPaginas;

    @Column(name = "paginas_processadas", nullable = false)
    private Integer paginasProcessadas;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "ocr_engine", nullable = false, length = 32)
    private String ocrEngine;

    @Column(name = "idioma", nullable = false, length = 8)
    private String idioma;

    @Column(name = "confianca_media", precision = 5, scale = 2)
    private BigDecimal confiancaMedia;

    @Column(name = "revisao_requerida", nullable = false)
    private boolean revisaoRequerida;

    @Column(name = "operador_id")
    private Long operadorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", length = 4000)
    private String failureReason;
}
