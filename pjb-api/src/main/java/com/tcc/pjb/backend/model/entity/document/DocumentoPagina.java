package com.tcc.pjb.backend.model.entity.document;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_documento_pagina")
public class DocumentoPagina {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id")
    private DocumentoProcessual documento;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    
    @Column(name = "page_id", nullable = false, unique = true, length = 80)
    private String pageId;

    
    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "texto_extraido", columnDefinition = "TEXT")
    private String textoExtraido;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
}
