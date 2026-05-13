package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.DOCUMENTOS, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_especialidade_pericial")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecialidadePericial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;
    

    @Column(nullable = false, length = 150)
    private String descricao;
    

    @Column(nullable = false, length = 50)
    private String area;
    
}
