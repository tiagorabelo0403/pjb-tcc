package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.springframework.stereotype.Component;

@Component
public final class PjbRitoBehaviorResolver {

    public PjbRitoBehavior resolve(RitoProcessual rito) {
        RitoProcessual safeRito = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        if (safeRito.isJuizado()) return new PjbJuizadoRitoBehavior(safeRito);
        if (safeRito.isInfancia()) return new PjbInfanciaRitoBehavior(safeRito);
        if (safeRito.isAmbiental()) return new PjbAmbientalRitoBehavior(safeRito);
        if (safeRito.isAgrario()) return new PjbAgrarioRitoBehavior(safeRito);
        if (safeRito.isAutocompositivo()) return new PjbAutocompositivoRitoBehavior(safeRito);
        if (safeRito.isAdministrativo()) return new PjbAdministrativoRitoBehavior(safeRito);
        if (safeRito.isEspecialConstitucional()) return new PjbEspecialConstitucionalRitoBehavior(safeRito);
        if (safeRito.isInternacional()) return new PjbInternacionalRitoBehavior(safeRito);
        return switch (safeRito.getGrupoPrincipal()) {
            case JUIZADO -> new PjbJuizadoRitoBehavior(safeRito);
            case PENAL -> new PjbCriminalRitoBehavior(safeRito);
            case TRABALHISTA -> new PjbTrabalhistaRitoBehavior(safeRito);
            case ELEITORAL -> new PjbEleitoralRitoBehavior(safeRito);
            case MILITAR -> new PjbMilitarRitoBehavior(safeRito);
            case EXECUCAO_FISCAL -> new PjbFazendaRitoBehavior(safeRito);
            case EXECUCAO_PENAL -> new PjbExecucaoPenalRitoBehavior(safeRito);
            case PREVIDENCIARIO -> new PjbPrevidenciarioRitoBehavior(safeRito);
            case FALENCIA_RECUPERACAO -> new PjbEmpresarialRitoBehavior(safeRito);
            case FAMILIA -> new PjbCivilRitoBehavior(safeRito);
            default -> new PjbCivilRitoBehavior(safeRito);
        };
    }
}
