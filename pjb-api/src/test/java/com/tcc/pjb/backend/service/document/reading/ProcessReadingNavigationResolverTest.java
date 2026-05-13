package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessReadingNavigationResolverTest {

    private final ProcessReadingNavigationResolver resolver = new ProcessReadingNavigationResolver();

    @Test
    void resolveBuildsDecisionEvidenceAndCitationNodes() {
        Processo processo = new Processo();
        processo.setId(17L);
        processo.setRamoDireito(RamoDireito.PENAL);
        processo.setFaseAtual(FaseProcessual.RECURSAL);

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.ASSESSOR_JUDICIAL);

        DocumentoProcessual documento = new DocumentoProcessual();
        documento.setId(UUID.randomUUID());
        documento.setTitulo("Acórdão de Apelação Criminal");
        documento.setContentType("application/pdf");

        DocumentoPagina pagina = new DocumentoPagina();
        pagina.setDocumento(documento);
        pagina.setPageId("P-1");
        pagina.setPageNumber(3);
        pagina.setTextoExtraido("O acórdão aplica o art. 157 do CPP, cita precedente e determina intime-se a defesa. Há laudo pericial complementar.");

        ProcessReadingModeProfile modeProfile = new ProcessReadingModeProfile(
                "GABINETE_RECURSAL_INTENSIVO",
                "AMBAR_JURIDICO",
                "AMBAR_PROGRESSIVO",
                "CONTRASTE_REFORCADO",
                "112",
                "EXPANDIDO",
                "AGRUPAMENTO_POR_PECA_E_BLOCO",
                "MAPA_RECURSAL_E_PECA_CHAVE",
                "PROVA_CRONOLOGICA_E_ELEMENTOS_DE_AUTORIA",
                "TRILHA_DECISAO_RECURSO_CONTRARRAZOES",
                "MINUTA_PRECEDENTE_E_PECA_CHAVE",
                "ANOTACAO_LATERAL_E_FIXACAO",
                "BLOCOS_CURTOS_COM_RESPIRACAO_VISUAL",
                "SINOPSE_PROGRESSIVA_POR_BLOCO",
                1,
                200,
                88,
                false,
                true,
                true,
                List.of()
        );
        ProcessReadingPresetProfile presetProfile = new ProcessReadingPresetProfile(
                true,
                "MEDIUM",
                "LEITURA_ASSESSORIA_RECURSAL",
                "AMBAR_JURIDICO",
                112,
                1.78,
                0.95,
                0.003,
                66,
                8,
                "FOCO_PROGRESSIVO_POR_BLOCO",
                "SEM_MASCARA",
                "ATALHOS_E_FOCO_FIXO",
                "LINHA_DO_TEMPO_DECISAO_RECURSO_CONTRARRAZOES",
                "MAPA_ARTIGOS_PRECEDENTES_E_TEMAS",
                "MINUTA_PRECEDENTE_E_ENFRENTAMENTO",
                "BUSCA_SEMANTICA_POR_PECA_E_PAGINA",
                "ANCORAS_RECURSAIS_FIXAS"
        );

        ProcessReadingNavigationResponse response = resolver.resolve(
                processo,
                usuario,
                List.of(documento),
                List.of(pagina),
                modeProfile,
                presetProfile
        );

        assertEquals(17L, response.processoId());
        assertFalse(response.nodes().isEmpty());
        assertTrue(response.nodes().stream().anyMatch(node -> node.nodeType().equals("DECISAO_CHAVE")));
        assertTrue(response.nodes().stream().anyMatch(node -> node.nodeType().equals("CITACAO_NORMATIVA")));
        assertTrue(response.nodes().stream().anyMatch(node -> node.nodeType().equals("PROVA_RELEVANTE")));
    }
}
