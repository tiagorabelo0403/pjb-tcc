package com.tcc.pjb.backend.service.secretariat.query.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretariatHearingMediaLaneServiceTest {

    private final SecretariatHearingMediaLaneService service = new SecretariatHearingMediaLaneService();

    @Test
    void shouldRouteLabourMediaToProcessualMediaDesk() {
        SecretariatHearingMediaLaneService.HearingMediaLaneSnapshot snapshot = service.resolve(
                "TRT7:1G:COMUM:CE:FORTALEZA",
                "MIDIA_AUDIENCIA",
                "Mídia trabalhista com gravação e transcrição de audiência",
                List.of("trabalhista", "video", "transcricao"),
                new ForumDeskPortfolioProfile("MESA_TRIAGEM", "MESA_GABINETE", "MESA_MIDIAS", "MESA_COMPLIANCE", "MESA_ESCALACAO", "MESA_ASSISTENTE", "MESA_COORD", "MESA_REDIST", "DASH_TRT", List.of(), new LinkedHashMap<>()),
                new SecretariatFlowBridgeProfile("ORIGINARIO", "LOCAL", "MESA_DISTRIBUICAO", "MESA_GABINETE", "MESA_RECURSAL", "MESA_ADMISSIBILIDADE", false, false, false, List.of(), new LinkedHashMap<>()),
                new SecretariatJudicialIntegrationProfile("PJB_INTERNAL", "MESA_PROTOCOLO", "INTERNAL", "NONE", "FULL", "SYNC", "Q_EXT", "MESA_REVISAO", "PJB_LOCAL", "ACK", "MESA_REPLAY", "RETRY", "EVIDENCIA", "JANELA", "TRT", "TRT7", "PJB_INTERNAL", "TRABALHISTA", null, "AUTOMATICO", "LOCAL", "MESA_CONTINGENCIA", "DLQ", "RETENCAO", "MESA_MANUAL", "TELEMETRIA", "AUDIT", "DLQ", "MESA_RECONCILIACAO", "AUDITADO", "SLA_8H", "MESA_ESCALACAO", "MESA_RECIBO", "PROVAS", "24H", false, false, List.of(), List.of(), new LinkedHashMap<>())
        );

        assertThat(snapshot.mediaRelated()).isTrue();
        assertThat(snapshot.indexingMode()).isEqualTo("TRANSCRICAO_ANCORAS_EVENTOS");
        assertThat(snapshot.targetDesk()).isEqualTo("MESA_MIDIAS");
    }

    @Test
    void shouldReflectCollegiateSessionIntoAgendaAndDesk() {
        SecretariatHearingMediaLaneService.HearingMediaLaneSnapshot snapshot = service.resolve(
                "TJCE:2G:COLEGIADO:CE:FORTALEZA",
                "SESSAO_SUSTENTACAO",
                "Sessão colegiada com sustentação oral e vídeo",
                List.of("sessao", "sustentacao_oral", "video"),
                new ForumDeskPortfolioProfile("MESA_TRIAGEM", "MESA_GABINETE", "MESA_AUDIENCIA", "MESA_COMPLIANCE", "MESA_COLEGIADA", "MESA_ASSISTENTE", "MESA_COORD", "MESA_REDIST", "DASH_2G", List.of(), new LinkedHashMap<>()),
                new SecretariatFlowBridgeProfile("RECURSAL", "COLEGIADO", "MESA_DISTRIBUICAO", "MESA_GABINETE", "MESA_RECURSAL", "MESA_ADMISSIBILIDADE", false, true, true, List.of(), new LinkedHashMap<>()),
                new SecretariatJudicialIntegrationProfile("EPROC", "MESA_PROTOCOLO", "REST", "CERT", "FULL", "SYNC", "Q_EXT", "MESA_REVISAO", "EPROC_CONNECTOR", "ACK", "MESA_REPLAY", "RETRY", "EVIDENCIA", "JANELA", "TRF5", "TRF5", "EPROC", "FEDERAL", "https://connector", "AUTOMATICO", "MANUAL", "MESA_CONTINGENCIA", "DLQ", "RETENCAO", "MESA_MANUAL", "TELEMETRIA", "AUDIT", "DLQ", "MESA_RECONCILIACAO", "AUDITADO", "SLA_8H", "MESA_ESCALACAO", "MESA_RECIBO", "PROVAS", "24H", false, false, List.of(), List.of(), new LinkedHashMap<>())
        );

        assertThat(snapshot.sessionRelated()).isTrue();
        assertThat(snapshot.connectorMediaDecision()).isEqualTo("SINCRONIZAR_MARCADORES_COM_CONECTOR");
        assertThat(snapshot.agendaReflection()).isEqualTo("AGENDA_FILA_MESA");
    }
}
