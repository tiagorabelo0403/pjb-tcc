package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PjbDataOwnership(module = PjbModuleId.COMPETENCIA_ROTEAMENTO, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "municipios")
public class Municipios {

    
    @Id
    @Column(name = "ibge_code", updatable = false, nullable = false)
    private Long ibgeCode;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 2)
    private String uf; 
}