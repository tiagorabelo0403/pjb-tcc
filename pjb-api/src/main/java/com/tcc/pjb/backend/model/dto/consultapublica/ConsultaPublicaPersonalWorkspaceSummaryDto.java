package com.tcc.pjb.backend.model.dto.consultapublica;

public record ConsultaPublicaPersonalWorkspaceSummaryDto(
        long totalProcessos,
        long comPrazoMonitorado,
        long comPrazoCritico,
        long comEtiquetas,
        long comMovimentacaoRecente,
        long comRotasInteligentes
) {
}
