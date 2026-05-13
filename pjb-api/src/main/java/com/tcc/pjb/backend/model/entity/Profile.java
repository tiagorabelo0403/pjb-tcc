package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.*;
import com.tcc.pjb.backend.model.entity.enums.Ramo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia;

@Getter
@Setter
@NoArgsConstructor
@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "ia_profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Ramo ramo;

    @Enumerated(EnumType.STRING)
    private Especie especie;

    @Enumerated(EnumType.STRING)
    private Competencia competencia;

    public Profile(Long id) {
        this.id = id;
    }


    @jakarta.persistence.Transient
    public String getNome() {
        
        String r = (ramo == null) ? "N/A" : ramo.name();
        String e = (especie == null) ? "N/A" : especie.name();
        String c = (competencia == null) ? "N/A" : competencia.name();
        return r + "-" + e + "-" + c;
    }
}
