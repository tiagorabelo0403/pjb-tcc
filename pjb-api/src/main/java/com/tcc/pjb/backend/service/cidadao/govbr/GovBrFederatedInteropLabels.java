package com.tcc.pjb.backend.service.cidadao.govbr;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorTlsMode;
import java.util.Locale;

public final class GovBrFederatedInteropLabels {

    private GovBrFederatedInteropLabels() {
    }

    public static final String DESCOBERTA_CPF_CNJ = "CPF_CANONICO_E_NUMERO_CNJ";
    public static final String DESCOBERTA_CPF_FONTES_HIBRIDAS = "CPF_CANONICO_FONTES_HIBRIDAS";
    public static final String ACESSO_CAPA_TIMELINE = "CAPA_TIMELINE_EVENTOS";
    public static final String ACESSO_CAPA_MINIMA = "CAPA_MINIMA_COM_LINK_FEDERADO";
    public static final String DOCUMENTO_PROXY_SOBERANO = "PROXY_SOBERANO_CONTROLADO";
    public static final String DOCUMENTO_LINK_FEDERADO = "LINK_FEDERADO_CONTROLADO";
    public static final String DOCUMENTO_ESPELHO_AUTORIZADO = "ESPELHO_AUTORIZADO_SOBERANO";
    public static final String DOCUMENTO_INDISPONIVEL = "SEM_DOCUMENTO_DIRETO";
    public static final String SINCRONIZACAO_INCREMENTAL = "SINCRONIZACAO_INCREMENTAL_GOVERNADA";
    public static final String SINCRONIZACAO_PROJECAO_LOCAL = "PROJECAO_LOCAL_COM_RECONCILIACAO";
    public static final String DECISAO_ACESSO_LIBERADO = "LIBERADO";
    public static final String DECISAO_ACESSO_STEP_UP = "EXIGIR_STEP_UP";
    public static final String DECISAO_ACESSO_NEGADO = "NEGADO";
    public static final String DECISAO_ACESSO_DEGRADADO = "DEGRADADO";

    public static String systemLabel(JudicialSystem system) {
        return GovBrCitizenPanelLabels.sourceLabel(system == null ? null : system.name());
    }

    public static String tlsLabel(JudicialConnectorTlsMode mode) {
        if (mode == null) {
            return "DESCONHECIDO";
        }
        return switch (mode) {
            case MTLS -> "MTLS";
            case TLS -> "TLS";
            default -> mode.name();
        };
    }

    public static String gapSeverity(String runtimeStatus) {
        String normalized = runtimeStatus == null ? "" : runtimeStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BLOCKED" -> "CRITICO";
            case "QUARANTINED", "STALE_READ_ONLY" -> "ALTO";
            case "DEGRADED" -> "MEDIO";
            default -> "BAIXO";
        };
    }
}
