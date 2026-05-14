package com.tcc.pjb.backend.service.institutional;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record CompetenciaInstitucional(
        UUID instituicaoId,
        Set<MateriaJurisdicao> materias,
        Set<RitoProcessual> ritos,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo,
        String territorioAbrangido
) {

    public CompetenciaInstitucional {
        materias = Set.copyOf(materias);
        ritos = Set.copyOf(ritos);
    }

    public boolean aceitaMateria(MateriaJurisdicao materia) {
        return materias.contains(materia);
    }

    public boolean aceitaRito(RitoProcessual rito) {
        return ritos.contains(rito);
    }

    public boolean aceitaValor(BigDecimal valor) {
        if (valor == null) return true;
        if (valorMinimo != null && valor.compareTo(valorMinimo) < 0) return false;
        if (valorMaximo != null && valor.compareTo(valorMaximo) > 0) return false;
        return true;
    }

    public boolean eCompetente(MateriaJurisdicao materia, RitoProcessual rito, BigDecimal valor) {
        return aceitaMateria(materia) && aceitaRito(rito) && aceitaValor(valor);
    }

    public static CompetenciaInstitucional irrestrita(UUID instituicaoId, String territorio) {
        return new CompetenciaInstitucional(
                instituicaoId,
                EnumSet.allOf(MateriaJurisdicao.class),
                EnumSet.allOf(RitoProcessual.class),
                null,
                null,
                territorio
        );
    }
}
