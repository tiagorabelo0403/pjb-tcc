package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialProfileResolverService {

    public CalculoJudicialSolicitantePerfil resolve(Authentication authentication, CalculoJudicialSolicitantePerfil requested) {
        if (requested != null) {
            return requested;
        }
        if (authentication == null || authentication.getAuthorities() == null) {
            return CalculoJudicialSolicitantePerfil.CIDADAO;
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority() == null ? "" : a.getAuthority().toUpperCase(Locale.ROOT))
                .toList();
        if (roles.stream().anyMatch(role -> role.endsWith("CIDADAO"))) {
            return CalculoJudicialSolicitantePerfil.CIDADAO;
        }
        if (roles.stream().anyMatch(role -> role.contains("CONTADOR") || role.contains("CALCULISTA") || role.contains("PERITO_CONTABIL"))) {
            return CalculoJudicialSolicitantePerfil.CONTADOR_JUDICIAL;
        }
        if (roles.stream().anyMatch(role -> role.endsWith("JUIZ") || role.endsWith("MAGISTRADO") || role.endsWith("DESEMBARGADOR") || role.endsWith("MINISTRO"))) {
            return CalculoJudicialSolicitantePerfil.MAGISTRATURA;
        }
        if (roles.stream().anyMatch(role -> role.endsWith("PROCURADOR") || role.contains("PROCURADORIA"))) {
            return CalculoJudicialSolicitantePerfil.PROCURADORIA;
        }
        if (roles.stream().anyMatch(role -> role.endsWith("SERVIDOR") || role.endsWith("SERVIDOR_FORUM") || role.contains("TECNICO"))) {
            return CalculoJudicialSolicitantePerfil.TECNICO_INSTITUCIONAL;
        }
        return CalculoJudicialSolicitantePerfil.ADVOGADO;
    }
}
