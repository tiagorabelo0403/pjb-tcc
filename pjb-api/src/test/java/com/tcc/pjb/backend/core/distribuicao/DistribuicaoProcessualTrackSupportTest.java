package com.tcc.pjb.backend.core.distribuicao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService.RoutingDecision;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class DistribuicaoProcessualTrackSupportTest {

    private final DistribuicaoProcessualTrackSupport support = new DistribuicaoProcessualTrackSupport();

    @Test
    void deveClassificarFluxoDeCustodiaComAlertaEFundamentoEspecializado() {
        var request = new DistribuicaoProcessualNacionalEngine.DistribuicaoRequest(
                "0001234-55.2026.8.06.0001",
                "CE",
                "Fortaleza",
                RitoProcessual.ESPECIAL_HABEAS_CORPUS,
                0d,
                "Paciente",
                "Autoridade coatora",
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "Foro Central",
                "Plantão",
                "Plantão de custódia",
                "Liberdade",
                "Habeas corpus",
                "Habeas corpus",
                "Liberdade",
                "Custódia",
                null,
                null,
                "Habeas corpus",
                "Auto de prisão em flagrante",
                false,
                false,
                false,
                true,
                false,
                true,
                false
        );
        RoutingDecision routing = routing(RitoProcessual.ESPECIAL_HABEAS_CORPUS, RamoDireito.PENAL, GrauJurisdicao.PRIMEIRO_GRAU, TipoJustica.ESTADUAL,
                "CUSTODIA", "Vara de Custódia", "Fortaleza", "FORO_FORTALEZA", "ENVELOPE_CUSTODIA");

        String track = support.resolveSpecializedTrack(request, routing);
        List<String> alertas = support.buildSpecializedAlertas(request, routing, track);
        List<String> fundamentos = support.buildSpecializedFundamentos(request, routing, track);
        List<String> checklist = support.buildSpecializedReviewChecklist(request, routing, track);

        assertThat(track).isEqualTo("CUSTODIA");
        assertThat(alertas).anyMatch(item -> item.contains("triagem reforçada"));
        assertThat(alertas).anyMatch(item -> item.contains("Custódia exige conferência imediata"));
        assertThat(fundamentos).anyMatch(item -> item.contains("tutela imediata da liberdade"));
        assertThat(checklist).anyMatch(item -> item.contains("autoridade coatora"));
    }

    @Test
    void deveClassificarFluxoConstitucionalEmGrauColegiado() {
        var request = new DistribuicaoProcessualNacionalEngine.DistribuicaoRequest(
                "ADI-2026",
                "DF",
                "Brasília",
                RitoProcessual.COMUM_ORDINARIO,
                0d,
                "Legitimado",
                "Estado",
                GrauJurisdicao.SEGUNDO_GRAU,
                "Brasília",
                "Tribunal",
                "Origem",
                "Plenário",
                "Controle",
                "ADI",
                "ADI",
                "Controle",
                "Constitucional",
                null,
                null,
                "ADI",
                "Controle concentrado",
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
        RoutingDecision routing = routing(RitoProcessual.COMUM_ORDINARIO, RamoDireito.CONSTITUCIONAL, GrauJurisdicao.SEGUNDO_GRAU, TipoJustica.ESTADUAL,
                "CONSTITUCIONAL", "Plenário", "Capital", "TJ", "ENVELOPE_CONSTITUCIONAL");

        String track = support.resolveSpecializedTrack(request, routing);

        assertThat(track).isEqualTo("CONSTITUCIONAL");
        assertThat(support.buildSpecializedFundamentos(request, routing, track))
                .anyMatch(item -> item.contains("Controle concentrado"));
    }

    private RoutingDecision routing(RitoProcessual rito,
                                    RamoDireito ramo,
                                    GrauJurisdicao grau,
                                    TipoJustica tipoJustica,
                                    String specializationAxis,
                                    String orgao,
                                    String comarca,
                                    String foro,
                                    String competenceEnvelope) {
        return new RoutingDecision(
                rito,
                ramo,
                grau,
                tipoJustica,
                "TJCE",
                "Tribunal",
                "ESTADUAL",
                "PJB",
                "PJB",
                grau.name(),
                orgao,
                "UNIDADE_01",
                "FILA_DISTRIBUICAO",
                false,
                true,
                false,
                4,
                BigDecimal.ZERO,
                comarca,
                comarca,
                foro,
                null,
                null,
                null,
                "TERRITORIAL_PADRAO",
                "PREVENCAO_PADRAO",
                "SORTEIO",
                specializationAxis,
                "ALEATORIO",
                "LIVRE",
                competenceEnvelope,
                "MEDIO",
                "DISTRIBUICAO",
                "MESA_DISTRIBUICAO",
                List.of(),
                List.of(),
                List.of(),
                new LinkedHashMap<>()
        );
    }
}
