package com.tcc.pjb.backend.core.procedural.rito.behavior;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;

public sealed interface PjbRitoBehavior
        permits PjbCivilRitoBehavior,
                PjbJuizadoRitoBehavior,
                PjbCriminalRitoBehavior,
                PjbTrabalhistaRitoBehavior,
                PjbEleitoralRitoBehavior,
                PjbMilitarRitoBehavior,
                PjbFazendaRitoBehavior,
                PjbInfanciaRitoBehavior,
                PjbPrevidenciarioRitoBehavior,
                PjbEmpresarialRitoBehavior,
                PjbAmbientalRitoBehavior,
                PjbAgrarioRitoBehavior,
                PjbAutocompositivoRitoBehavior,
                PjbAdministrativoRitoBehavior,
                PjbExecucaoPenalRitoBehavior,
                PjbEspecialConstitucionalRitoBehavior,
                PjbInternacionalRitoBehavior {

    RitoProcessual rito();

    String prazoPolicy();

    String movementPolicy();

    boolean simplifiedProcedure();

    boolean allowsEdital();

    boolean costsAtFirstDegree();

    boolean digitalHearingDefault();

    boolean requiresHumanReview();

    List<String> defaultVisibleActions();

    List<String> defaultHiddenActions();

    List<String> defaultValidations();

    List<String> firstMovements(int urgencyScore);
}
