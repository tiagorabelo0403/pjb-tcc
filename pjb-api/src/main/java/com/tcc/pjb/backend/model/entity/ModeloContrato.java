package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;

@Getter
@Setter
@NoArgsConstructor
@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "modelos_contrato")
public class ModeloContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MateriaJurisdicao materia;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String templateHtml;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FaseProcessual faseProcessual;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorMaximo;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean ativo = true;
}
