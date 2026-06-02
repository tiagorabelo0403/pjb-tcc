package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessReadingProceduralContextResolverTest {

    private final ProcessReadingProceduralContextResolver resolver = new ProcessReadingProceduralContextResolver();

    @Test
    void resolveBuildsRecursalHtmlAwareContext() {
        Processo processo = Processo.builder()
                .id(42L)
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.PENAL)
                .rito(RitoProcessual.PENAL_REVISAO_CRIMINAL)
                .faseAtual(FaseProcessual.RECURSAL)
                .tribunalCodigoRoteado("TJCE")
                .unidadeJudiciariaCodigo("2CAMCRIM")
                .build();

        ProcessReadingModeProfile modeProfile = new ProcessReadingModeProfile(
                "GABINETE_RECURSAL_INTENSIVO", "AMBAR_RESERVADO", "AMBAR_PROGRESSIVO", "CONTRASTE_REFORCADO", "108", "PADRAO_LIMPO",
                "AGRUPAMENTO_POR_PECA_E_BLOCO", "MAPA_RECURSAL_E_PECA_CHAVE", "PROVA_CRONOLOGICA_E_ELEMENTOS_DE_AUTORIA", "TRILHA_DECISAO_RECURSO_CONTRARRAZOES",
                "TRILHA_DECISORIA_E_CHECKLIST_DE_ENFRENTAMENTO", "ANOTACAO_LATERAL_E_FIXACAO", "BLOCOS_CURTOS_COM_RESPIRACAO_VISUAL", "SINOPSE_PROGRESSIVA_POR_BLOCO",
                3, 120, 82, true, true, true, List.of());
        ProcessReadingFlowResponse processFlow = new ProcessReadingFlowResponse(
                4L, 2L, 1L, 1L, "LINHA_DO_TEMPO_PROCESSUAL", "ATOS_E_RECURSOS_EM_LINHA_DO_TEMPO", List.of(), null);
        ProcessReadingNavigationResponse navigation = new ProcessReadingNavigationResponse(
                42L, "MAPA_RECURSAL_E_PECA_CHAVE", "LINHA_DO_TEMPO_PROCESSUAL", 6, List.of(), Map.of());

        ProcessReadingProceduralContextResponse response = resolver.resolve(processo, modeProfile, processFlow, navigation, 2L, 120L);

        assertEquals("JUSTICA_ESTADUAL", response.justiceTrack());
        assertEquals("SEGUNDO_GRAU", response.tribunalTier());
        assertEquals("RECURSAL_SEM_MAPA_COMPLETO", response.recursalTrack());
        assertTrue(response.markers().contains("HTML_INLINE_PRIORITARIO"));
        assertTrue(response.metadata().containsKey("supportsResourceAndEmbargoReading"));
    }
}
