package com.tcc.pjb.backend.model.dto.projections;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;

public interface JurisdicaoContextProjection {
    Long getId();
    String getNome();
    String getSigla();
    MateriaJurisdicao getMateria();
}
