package com.tcc.pjb.backend.model.dto.cidadao.govbr;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoGovBrInteroperabilidadeFederadaResponse(
        LocalDateTime generatedAt,
        IdentitySummary identidade,
        Summary summary,
        List<ConnectorCoverage> conectores,
        List<GapItem> gaps,
        List<String> alertasEstruturais,
        Links links
) {

    public record IdentitySummary(
            boolean govBrEnabled,
            boolean govBrLinked,
            String govBrNivel,
            boolean discoveryReady,
            boolean acessoRestritoReady,
            boolean stepUpDisponivel,
            LocalDateTime ultimaSincronizacaoIdentidade
    ) {
    }

    public record Summary(
            int totalConectores,
            int conectoresDescobertaProntos,
            int conectoresAcessoProntos,
            int conectoresDocumentoProntos,
            int conectoresBloqueados,
            int conectoresQuarentenados,
            int conectoresComStepUp,
            int conectoresComMtls,
            int conectoresComProxySoberano
    ) {
    }

    public record ConnectorCoverage(
            String sistemaOrigem,
            String sistemaLabel,
            boolean connectorRegistrado,
            String modoDescoberta,
            String modoAcesso,
            String modoDocumento,
            String modoSincronizacao,
            String runtimeStatus,
            String tlsMode,
            boolean discoveryReady,
            boolean accessReady,
            boolean documentReady,
            boolean exigeStepUp,
            boolean exigeCertificado,
            boolean transporteSeguroPronto,
            boolean proxySoberanoElegivel,
            boolean runtimeBloqueado,
            List<String> alertas,
            List<String> bloqueios,
            String bridgeUrl
    ) {
    }

    public record GapItem(
            String codigo,
            String severidade,
            String titulo,
            String descricao
    ) {
    }

    public record Links(
            String acervoUnificadoUrl,
            String accessPolicyUrl,
            String govBrAssuranceUrl,
            String govBrStepUpStartUrl,
            String connectorDiagnosticUrl
    ) {
    }
}
