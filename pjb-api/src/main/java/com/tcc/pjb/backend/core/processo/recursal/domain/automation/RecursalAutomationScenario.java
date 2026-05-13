package com.tcc.pjb.backend.core.processo.recursal.domain.automation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RecursalAutomationScenario(
        RecursalPronunciamentoJudicial pronunciamentoJudicial,
        RecursalPretensaoRecursal pretensaoRecursal,
        boolean sucumbenciaReciproca,
        boolean recursoPrincipalInterposto,
        boolean recursoPrincipalConhecido,
        boolean processoFisico,
        boolean autosEletronicos,
        boolean preparoEfetuado,
        boolean preparoInsuficiente,
        boolean feriadoLocalAplicavel,
        boolean feriadoLocalComprovado,
        boolean pretendeEfeitoInfringente,
        boolean contrarrazoesJaProtocoladas,
        boolean inadmissaoRecursoExcepcional,
        boolean divergenciaJurisprudencialInterna,
        boolean renunciaDireitoRecorrer,
        boolean desistiuRecursoInterposto,
        boolean aquiescenciaExpressaOuTacita,
        boolean desejaSustentacaoOral,
        boolean juizadoEspecial,
        Set<String> fundamentosEmbargos) {

    public RecursalAutomationScenario {
        pronunciamentoJudicial = Objects.requireNonNull(pronunciamentoJudicial, "pronunciamentoJudicial");
        pretensaoRecursal = Objects.requireNonNull(pretensaoRecursal, "pretensaoRecursal");
        Objects.requireNonNull(fundamentosEmbargos, "fundamentosEmbargos");
        fundamentosEmbargos = Set.copyOf(new LinkedHashSet<>(fundamentosEmbargos));
    }

    public boolean hasEmbargosGrounds() {
        return !fundamentosEmbargos.isEmpty();
    }

    public boolean hasPowerToAppealBlockingEvent() {
        return renunciaDireitoRecorrer || desistiuRecursoInterposto || aquiescenciaExpressaOuTacita;
    }
}
