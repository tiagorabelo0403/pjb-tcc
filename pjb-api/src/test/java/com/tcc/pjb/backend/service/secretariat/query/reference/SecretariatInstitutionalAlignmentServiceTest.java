package com.tcc.pjb.backend.service.secretariat.query.reference;

import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretariatInstitutionalAlignmentServiceTest {

    private final SecretariatInstitutionalAlignmentService service = new SecretariatInstitutionalAlignmentService();

    @Test
    void shouldResolveElectoralInstitutionalCells() {
        SecretariatSpecializationProfile specialization = new SecretariatSpecializationProfile(
                "SECRETARIA_SEGUNDA_INSTANCIA_ELEITORAL",
                "SEGUNDA_INSTANCIA",
                "ELEITORAL",
                "PJB_SEGUNDA_INSTANCIA",
                "PJB Segunda Instância | Eleitoral",
                "PJB_SEGUNDA_INSTANCIA:ELEITORAL:SEGUNDA_INSTANCIA",
                "SECRETARIA_TRE_CE_ELEITORAL",
                "Secretaria Judiciária Eleitoral de Segunda Instância - TRE-CE",
                "SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA",
                List.of("PAUTA_COLEGIADA", "CORREGEDORIA_ELEITORAL_PJB"),
                new LinkedHashMap<>()
        );
        SecretariatFlowBridgeProfile bridgeProfile = new SecretariatFlowBridgeProfile(
                "COLEGIADO",
                "LOCALIZADOR",
                "COLEGIADO",
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
                "PJE",
                "ELEITORAL",
                "https://tre-ce.jus.br",
                "COLEGIADO",
                "PJB_ONLY",
                "CONTINGENCIA",
                "DLQ.REPLAY",
                "RETENCAO_PADRAO",
                "BALCAO",
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

        SecretariatInstitutionalAlignmentService.InstitutionalAlignmentSnapshot snapshot = service.resolve(
                "SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA",
                "SECRETARIA_TRE:PAUTA:ACORDAO:BAIXA",
                specialization,
                bridgeProfile,
                integrationProfile
        );

        assertThat(snapshot.institutionalAxis()).isEqualTo("ELEITORAL_JUDICIARIO");
        assertThat(snapshot.cells()).contains("CARTORIO_ELEITORAL", "AUTUACAO_DISTRIBUICAO_INFORMACOES_PROCESSUAIS", "CORREGEDORIA_ELEITORAL_PJB");
        assertThat(snapshot.touchpoints()).contains("CORREGEDORIA_ELEITORAL", "SUSTENTACAO_ORAL");
        assertThat(snapshot.gaps()).doesNotContain("alinhamento-eleitoral-institucional-ainda-nao-explicito");
    }
}
