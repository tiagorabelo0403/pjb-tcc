package com.tcc.pjb.backend.model.dto.processual.recursal.automation;

import java.util.List;

public record RecursalAutomationResponse(
        String pronunciamentoJudicial,
        String pretensaoRecursal,
        List<RecursalAutomationCandidateView> candidatos,
        List<RecursalAutomationSignalView> sinais,
        boolean admiteRecursoAdesivo,
        String observacaoRecursoAdesivo,
        boolean poderRecorrerBloqueado,
        String motivoBloqueioPoderRecorrer) {
}
