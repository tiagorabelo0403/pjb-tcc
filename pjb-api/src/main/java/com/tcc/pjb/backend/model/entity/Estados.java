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
@Table(name = "estados")
public class Estados {

    
    @Id
    @Column(length = 2, updatable = false, nullable = false)
    private String uf;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private Boolean ativo = true;
}