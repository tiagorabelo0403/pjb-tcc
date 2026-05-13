package com.tcc.pjb.backend.service.secretariat.query.operational;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretariatOperationalActionModelServiceTest {

    private final SecretariatOperationalDeskModelService deskService = new SecretariatOperationalDeskModelService();
    private final SecretariatOperationalActionModelService actionService = new SecretariatOperationalActionModelService();

    @Test
    void shouldExposeCollegiateAndElectoralOperationalActions() {
        SecretariatSpecializationProfile specialization = new SecretariatSpecializationProfile(
                "SECRETARIA_ELEITORAL_SEGUNDA_INSTANCIA",
                "SEGUNDA_INSTANCIA",
                "ELEITORAL",
                "PJB.SECRETARIA.ELEITORAL",
                "PAINEL_SECRETARIA_ELEITORAL",
                "secretaria-eleitoral",
                "SECRETARIA_ELEITORAL_SEGUNDA_INSTANCIA",
                "Secretaria Judiciária Eleitoral",
                "SEC:TRE-CE",
                List.of("COLEGIADO"),
                new LinkedHashMap<>()
        );
        SecretariatJudicialIntegrationProfile integrationProfile = electoralIntegration();
        SecretariatOperationalDeskModelService.OperationalDeskSnapshot deskSnapshot = deskService.resolve(
                "SEC:TRE-CE:SEGUNDO_GRAU:COMUM:CE:fortaleza:eleitoral",
                "SECRETARIA_TRE:PAUTA:ACORDAO",
                specialization,
                portfolio(),
                deskProfile(),
                bridgeProfile(),
                integrationProfile
        );

        SecretariatOperationalActionModelService.OperationalActionSnapshot snapshot = actionService.resolve(
                "SEC:TRE-CE:SEGUNDO_GRAU:COMUM:CE:fortaleza:eleitoral",
                "SECRETARIA_TRE:PAUTA:ACORDAO",
                specialization,
                deskSnapshot,
                integrationProfile
        );

        assertThat(snapshot.actions()).extracting(SecretariatOperationalActionModelService.OperationalDeskActionView::actionCode)
                .contains("INCLUIR_EM_PAUTA", "PUBLICAR_ACORDAO", "AUTUAR_FEITO_ELEITORAL", "INSTAURAR_PROCEDIMENTO_CORREGEDOR");
        assertThat(snapshot.labels()).contains("ACOES_COM_EVIDENCIA", "ACOES_EM_LOTE");
        assertThat(snapshot.diagnostics()).containsEntry("supportsElectoralCorregedoriaActions", true);
    }

    @Test
    void shouldExposeMilitaryOperationalActions() {
        SecretariatSpecializationProfile specialization = new SecretariatSpecializationProfile(
                "SECRETARIA_MILITAR_SEGUNDA_INSTANCIA",
                "SEGUNDA_INSTANCIA",
                "MILITAR",
                "PJB.SECRETARIA.MILITAR",
                "PAINEL_SECRETARIA_MILITAR",
                "secretaria-militar",
                "SECRETARIA_MILITAR_SEGUNDA_INSTANCIA",
                "Secretaria Judiciária Militar",
                "SEC:STM",
                List.of("COLEGIADO"),
                new LinkedHashMap<>()
        );
        SecretariatJudicialIntegrationProfile integrationProfile = militaryIntegration();
        SecretariatOperationalDeskModelService.OperationalDeskSnapshot deskSnapshot = deskService.resolve(
                "SEC:STM:SEGUNDO_GRAU:COMUM:DF:brasilia:militar",
                "SECRETARIA_STM:PLANTAO:SESSAO_MILITAR",
                specialization,
                portfolio(),
                deskProfile(),
                bridgeProfile(),
                integrationProfile
        );

        SecretariatOperationalActionModelService.OperationalActionSnapshot snapshot = actionService.resolve(
                "SEC:STM:SEGUNDO_GRAU:COMUM:DF:brasilia:militar",
                "SECRETARIA_STM:PLANTAO:SESSAO_MILITAR",
                specialization,
                deskSnapshot,
                integrationProfile
        );

        assertThat(snapshot.actions()).extracting(SecretariatOperationalActionModelService.OperationalDeskActionView::actionCode)
                .contains("RECEBER_URGENCIA_PLANTAO", "REGISTRAR_ATENDIMENTO_VIRTUAL", "REGISTRAR_RESULTADO_SESSAO_MILITAR");
        assertThat(snapshot.gaps()).doesNotContain("acao-militar-ainda-sem-conector-eproc-explicito");
        assertThat(snapshot.diagnostics()).containsEntry("supportsPlantaoMilitarActions", true);
    }

    private ForumDeskPortfolioProfile portfolio() {
        return new ForumDeskPortfolioProfile(
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
    }

    private SecretariatDeskLoadProfile deskProfile() {
        return new SecretariatDeskLoadProfile(
                "SEC:BASE",
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
    }

    private SecretariatFlowBridgeProfile bridgeProfile() {
        return new SecretariatFlowBridgeProfile(
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
    }

    private SecretariatJudicialIntegrationProfile electoralIntegration() {
        return new SecretariatJudicialIntegrationProfile(
                "PJE_TRE",
                "AUTUACAO_DISTRIBUICAO",
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
                "PJE_PJECOR",
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
    }

    private SecretariatJudicialIntegrationProfile militaryIntegration() {
        return new SecretariatJudicialIntegrationProfile(
                "EPROC_JMU",
                "PROTOCOLO_MILITAR",
                "SISTEMA",
                "CERTIFICADO",
                "PADRAO",
                "ASYNC",
                "EXT",
                "REVISAO",
                "STM-CONNECTOR",
                "CIENCIA_SISTEMA",
                "REPLAY",
                "EXPONENTIAL",
                "EVIDENCIA",
                "JANELA_24H",
                "STM",
                "Superior Tribunal Militar",
                "EPROC",
                "MILITAR",
                "https://stm.jus.br",
                "COLEGIADO",
                "PJB_ONLY",
                "CONTINGENCIA",
                "DLQ.REPLAY",
                "RETENCAO_PADRAO",
                "BALCAO_PLANTAO_MILITAR",
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
                List.of("MILITAR"),
                new LinkedHashMap<>()
        );
    }
}
