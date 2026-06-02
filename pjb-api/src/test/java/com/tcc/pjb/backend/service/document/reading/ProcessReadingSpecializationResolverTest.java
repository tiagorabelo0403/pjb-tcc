package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingDocumentResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSpecializationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessReadingSpecializationResolverTest {

    private final ProcessReadingSpecializationResolver resolver = new ProcessReadingSpecializationResolver();

    @Test
    void resolveBuildsRecursalSpecializationWithNationalScope() {
        Processo processo = Processo.builder()
                .id(12L)
                .tipoJustica(TipoJustica.FEDERAL)
                .ramoDireito(RamoDireito.PREVIDENCIARIO)
                .rito(RitoProcessual.PREVIDENCIARIO_REVISAO_BENEFICIO)
                .faseAtual(FaseProcessual.RECURSAL)
                .tribunalCodigoRoteado("TRF5")
                .unidadeJudiciariaCodigo("1TURMA")
                .build();

        ProcessReadingModeProfile modeProfile = new ProcessReadingModeProfile(
                "GABINETE_RECURSAL_INTENSIVO", "AMBAR_RESERVADO", "AMBAR_PROGRESSIVO", "CONTRASTE_REFORCADO", "108", "PADRAO_LIMPO",
                "AGRUPAMENTO_POR_PECA_E_BLOCO", "MAPA_RECURSAL_E_PECA_CHAVE", "PROVA_CRONOLOGICA_E_ELEMENTOS_DE_AUTORIA", "TRILHA_DECISAO_RECURSO_CONTRARRAZOES",
                "TRILHA_DECISORIA_E_CHECKLIST_DE_ENFRENTAMENTO", "ANOTACAO_LATERAL_E_FIXACAO", "BLOCOS_CURTOS_COM_RESPIRACAO_VISUAL", "SINOPSE_PROGRESSIVA_POR_BLOCO",
                2, 22, 91, false, true, false, List.of());

        ProcessReadingFlowResponse processFlow = new ProcessReadingFlowResponse(4L, 3L, 1L, 0L, "LINHA_DO_TEMPO_PROCESSUAL", "ATOS_E_RECURSOS_EM_LINHA_DO_TEMPO", List.of(), null);
        ProcessReadingProceduralContextResponse proceduralContext = new ProcessReadingProceduralContextResponse(
                "JUSTICA_FEDERAL",
                "SEGUNDO_GRAU",
                "PREVIDENCIARIO",
                "PREVIDENCIARIO",
                "PREVIDENCIARIO_REVISAO_BENEFICIO",
                "PREVIDENCIARIO_E_SOCIAL",
                "RECURSAL",
                "RECURSAL_ORDINARIA",
                "1TURMA",
                "DECISAO_RECURSO_CONTRARRAZOES_E_JULGAMENTO",
                "EMBARGOS_E_INTEGRACAO_DECISORIA",
                "HTML_NATIVO_E_PDF_ASSINADO_HIBRIDOS",
                "ATO_HTML_COM_CONFERENCIA_PDF_ASSINADO",
                true,
                true,
                List.of(),
                Map.of()
        );
        List<ProcessReadingDocumentResponse> documents = List.of(new ProcessReadingDocumentResponse(
                UUID.randomUUID(),
                "Acórdão",
                "ACORDAO",
                "application/pdf",
                1024L,
                12L,
                100,
                "PDF_TEXTUAL_ASSISTIDO",
                List.of(),
                Map.of()
        ));

        ProcessReadingSpecializationResponse response = resolver.resolve(processo, modeProfile, processFlow, proceduralContext, documents);

        assertEquals("DECISAO_ATACADA_RAZOES_CONTRARRAZOES_VOTO_ACORDAO", response.decisionMode());
        assertEquals("APELACAO_AGRAVO_RECURSO_ORDINARIO_CONTRARRAZOES_VOTO_ACORDAO", response.resourceMode());
        assertTrue(response.nativeHtmlPriority());
        assertTrue(response.signedPdfInspectionRequired());
        assertTrue(response.metadata().containsKey("supportsAllBrazilianRites"));
        assertTrue(response.metadata().containsKey("supportsAllBrazilianRights"));
    }
}
