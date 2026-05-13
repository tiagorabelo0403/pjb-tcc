package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;

public record PjbOperationalScopeContext(
        String usuarioId,
        String perfil,
        String tribunalCode,
        String comarcaCode,
        String unidadeCode,
        String varaId,
        String competencia,
        List<RitoProcessual> ritosPermitidos,
        String grau,
        String orgaoJulgadorId,
        String equipeId,
        String escopoInstitucional
) {

    public List<RitoProcessual> safeRitosPermitidos() {
        return ritosPermitidos == null ? List.of() : List.copyOf(ritosPermitidos);
    }

    public boolean permiteRito(RitoProcessual rito) {
        if (rito == null) return false;
        List<RitoProcessual> permitidos = safeRitosPermitidos();
        return permitidos.isEmpty() || permitidos.contains(rito);
    }
}
