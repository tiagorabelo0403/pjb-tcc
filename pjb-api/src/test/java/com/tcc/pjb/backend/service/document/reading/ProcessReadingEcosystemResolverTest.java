package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSpecializationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessReadingEcosystemResolverTest {

    private final ProcessReadingEcosystemResolver resolver = new ProcessReadingEcosystemResolver();

    @Test
    void resolveBuildsPdpjConvergentMeshForPjeTribunal() {
        Processo processo = Processo.builder()
                .id(77L)
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.RECURSAL)
                .tribunalCodigoRoteado("TJCE")
                .build();

        ProcessReadingModeProfile modeProfile = new ProcessReadingModeProfile(
                "GABINETE_RECURSAL_INTENSIVO", "AMBAR_RESERVADO", "AMBAR_PROGRESSIVO", "CONTRASTE_REFORCADO", "108", "PADRAO_LIMPO",
                "AGRUPAMENTO_POR_PECA_E_BLOCO", "MAPA_RECURSAL_E_PECA_CHAVE", "PROVA_CRONOLOGICA_E_ELEMENTOS_DE_AUTORIA", "TRILHA_DECISAO_RECURSO_CONTRARRAZOES",
                "TRILHA_DECISORIA_E_CHECKLIST_DE_ENFRENTAMENTO", "ANOTACAO_LATERAL_E_FIXACAO", "BLOCOS_CURTOS_COM_RESPIRACAO_VISUAL", "SINOPSE_PROGRESSIVA_POR_BLOCO",
                6, 220, 54, false, true, true, List.of());

        ProcessReadingProceduralContextResponse proceduralContext = new ProcessReadingProceduralContextResponse(
                "JUSTICA_ESTADUAL",
                "SEGUNDO_GRAU",
                "CIVIL",
                "CIVIL",
                "COMUM_ORDINARIO",
                "COMUM_CIVEL",
                "RECURSAL",
                "RECURSAL_ORDINARIA",
                "CAMARA_CIVEL",
                "DECISAO_RECURSO_CONTRARRAZOES_E_JULGAMENTO",
                "EMBARGOS_E_INTEGRACAO_DECISORIA",
                "ATO_HTML_COM_CONFERENCIA_PDF_ASSINADO",
                "PFD_A_ASSINADO_COM_VERIFICACAO_FORMAL",
                true,
                true,
                List.of(),
                Map.of()
        );

        ProcessReadingSpecializationResponse specialization = new ProcessReadingSpecializationResponse(
                "SEGUNDO_GRAU_ESTADUAL_RECURSAL",
                "CAMARA_CIVEL_RECURSAL",
                "DECISAO_ATACADA_RAZOES_CONTRARRAZOES_VOTO_ACORDAO",
                "PROVA_DOCUMENTAL_E_CRONOLOGICA",
                "APELACAO_AGRAVO_RECURSO_ORDINARIO_CONTRARRAZOES_VOTO_ACORDAO",
                "EMBARGOS_DECLARACAO_E_INTEGRACAO_DO_JULGADO",
                "SESSAO_RECURSAL",
                "EXECUCAO_NAO_PRIORITARIA",
                "GABINETE_E_SECRETARIA_RECURSAL",
                true,
                true,
                List.of("DECISAO", "RAZOES", "CONTRARRAZOES", "VOTO", "ACORDAO"),
                List.of("ACORDAO_HTML_NATIVO", "EMBARGOS_DECLARACAO_HTML"),
                List.of(),
                Map.of()
        );

        var response = resolver.resolve(processo, modeProfile, proceduralContext, specialization);

        assertEquals("PJE", response.primarySystem());
        assertEquals("PDPJ", response.fallbackSystem());
        assertEquals("PDPJ_CONVERGENCIA_COM_HTML_NATIVO_PRIORITARIO", response.convergenceMode());
        assertEquals("ASSINATURA_NUVEM_MFA_COM_HTML_PRIMARIO_E_PDF_FORMAL", response.signatureMode());
        assertTrue(response.strategicCapabilities().contains("PAINEL_CONVERGENTE_COM_PDPJ_E_INTEROPERABILIDADE_NACIONAL"));
        assertTrue(response.frontend().supportsCloudSigning());
    }
}
