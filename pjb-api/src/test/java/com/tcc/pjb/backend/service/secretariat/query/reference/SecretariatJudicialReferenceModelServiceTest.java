package com.tcc.pjb.backend.service.secretariat.query.reference;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretariatJudicialReferenceModelServiceTest {

    private final SecretariatJudicialReferenceModelService service = new SecretariatJudicialReferenceModelService();

    @Test
    void shouldExposeTribunalElectoralReferenceModel() {
        ForumDeskPortfolioProfile portfolio = new ForumDeskPortfolioProfile(
                "TRIAGE",
                "GABINETE",
                "AUDIENCIA",
                "CUMPRIMENTO",
                "ESCALACAO",
                "ASSISTENTE",
                "COORDENACAO",
                "REDISTRIBUICAO",
                "SECRETARIA",
                List.of("BASE"),
                new LinkedHashMap<>()
        );
        SecretariatDeskLoadProfile deskProfile = new SecretariatDeskLoadProfile(
                "SEC:TRE-CE:SEGUNDO_GRAU:COMUM:CE:fortaleza:civel",
                "TRIAGE",
                12,
                1,
                0,
                0,
                0,
                "MODERATE",
                "REDISTRIBUICAO",
                "GABINETE",
                "FLOW_STANDARD",
                false,
                false,
                false,
                List.of("MODERATE"),
                new LinkedHashMap<>()
        );
        SecretariatFlowBridgeProfile bridgeProfile = new SecretariatFlowBridgeProfile(
                "COLEGIADO",
                "LOCALIZADOR",
                "DISTRIBUICAO",
                "GABINETE_RELATOR",
                "PAUTA",
                "ADMISSIBILIDADE",
                true,
                true,
                true,
                List.of("COLEGIADO"),
                new LinkedHashMap<>()
        );
        SecretariatJudicialIntegrationProfile integrationProfile = new SecretariatJudicialIntegrationProfile(
                "PJE_TRE",
                "PROTOCOLO",
                "SISTEMA",
                "CERTIFICADO",
                "PADRAO",
                "ASYNC",
                "EXT",
                "REVISAO",
                "TRE-CONNECTOR",
                "CIENCIA_SISTEMA",
                "REPLAY",
                "EXPONENTIAL",
                "EVIDENCIA",
                "JANELA_24H",
                "TRE-CE",
                "Tribunal Regional Eleitoral do Ceará",
                "PJE",
                "ELEITORAL",
                "https://tre-ce.jus.br",
                "COLEGIADO",
                "PJB_ONLY",
                "CONTINGENCIA",
                "DLQ.REPLAY",
                "RETENCAO_PADRAO",
                "MANUAL",
                "TELEMETRIA",
                "CANAL",
                "DLQ",
                "RECONCILIACAO",
                "AUDITORIA",
                "SLA_PROTOCOLO",
                "ESCALACAO",
                "RECIBO",
                "PRINCIPAL_ACESSORIO",
                "JANELA_48H",
                true,
                true,
                List.of(),
                List.of("ELEITORAL"),
                new LinkedHashMap<>()
        );

        SecretariatJudicialReferenceModelService.ReferenceModelSnapshot snapshot = service.resolve(
                "SEC:TRE-CE:SEGUNDO_GRAU:COMUM:CE:fortaleza:eleitoral",
                "SECRETARIA_TRE:PAUTA:ACORDAO",
                portfolio,
                deskProfile,
                bridgeProfile,
                integrationProfile
        );

        assertThat(snapshot.instanceClass()).isEqualTo("SEGUNDA_INSTANCIA");
        assertThat(snapshot.branchClass()).isEqualTo("ELEITORAL");
        assertThat(snapshot.models()).containsKeys("PJE", "E_SAJ", "EPROC", "TRIBUNAL_COLEGIADO", "ELEITORAL");
        assertThat(snapshot.gaps()).doesNotContain("fila-explicita-de-pauta-publicacao-e-sessao-colegiada");
        assertThat(snapshot.labels()).contains("COLEGIADO", "ELEITORAL");
        assertThat(snapshot.diagnostics()).containsEntry("supportsCollegiateAgenda", true);
    }
}
