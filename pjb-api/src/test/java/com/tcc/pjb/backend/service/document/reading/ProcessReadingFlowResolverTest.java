package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusEvento;
import com.tcc.pjb.backend.model.entity.enums.TipoEvento;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessReadingFlowResolverTest {

    private final ProcessReadingFlowResolver resolver = new ProcessReadingFlowResolver();

    @Test
    void resolveBuildsInlineMovementAndEventEntries() {
        Processo processo = new Processo();
        processo.setId(88L);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setResumoIA("Despacho saneador com determinação de intimação das partes e delimitação dos pontos controvertidos.");
        processo.setPedidosConsolidados("Condenação, tutela de urgência e produção de prova pericial.");
        processo.setMaterialProbatorioResumo("Laudo técnico, contrato e extratos bancários.");
        processo.setDataCriacao(LocalDateTime.now().minusDays(10));
        processo.setDataUltimaMovimentacao(LocalDateTime.now().minusHours(2));

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR);
        usuario.setNome("Servidor Teste");

        MovimentacaoProcessual movimentacao = MovimentacaoProcessual.builder()
                .id(12L)
                .processo(processo)
                .descricao("Despacho: intime-se a parte autora para manifestação em 5 dias.")
                .dataMovimentacao(Instant.now())
                .build();

        EventoProcessual evento = EventoProcessual.builder()
                .id(21L)
                .processo(processo)
                .responsavel(usuario)
                .tipo(TipoEvento.AUDIENCIA_INSTRUCAO)
                .status(StatusEvento.PENDENTE)
                .titulo("Audiência de instrução")
                .descricao("Audiência designada com prazo para manifestação prévia.")
                .dataInicio(LocalDateTime.now().plusDays(3))
                .dataFim(LocalDateTime.now().plusDays(3).plusHours(2))
                .build();

        ProcessReadingModeProfile modeProfile = new ProcessReadingModeProfile(
                "TRIAGEM_OPERACIONAL_ASSISTIDA",
                "AMBAR_JURIDICO",
                "AMBAR_PROGRESSIVO",
                "CONTRASTE_EQUILIBRADO",
                "108",
                "PADRAO_LIMPO",
                "AGRUPAMENTO_POR_DOCUMENTO",
                "MAPA_DE_PECAS_E_MARCADORES",
                "PROVA_GERAL",
                "TRILHA_LINEAR",
                "PENDENCIA_PRAZO_E_MOVIMENTACAO",
                "MARCADOR_SEMANTICO",
                "LEITURA_CONTINUA_SUAVE",
                "SINOPSE_DIRETA",
                0,
                0,
                0,
                false,
                false,
                false,
                List.of()
        );
        ProcessReadingPresetProfile presetProfile = new ProcessReadingPresetProfile(
                true,
                "MEDIUM",
                "LEITURA_SERVIDOR_MALHA_OPERACIONAL",
                "AMBAR_JURIDICO",
                108,
                1.62,
                0.84,
                0.002,
                72,
                6,
                "FOCO_DISCRETO_POR_PECA",
                "SEM_MASCARA",
                "ATALHOS_E_FOCO_FIXO",
                "LINHA_DO_TEMPO_OPERACIONAL",
                "MAPA_ARTIGOS_PRECEDENTES_E_TEMAS",
                "PRAZOS_PENDENCIAS_E_IMPULSO",
                "BUSCA_SEMANTICA_POR_PECA_E_PAGINA",
                "ANCORAS_OPERACIONAIS"
        );

        ProcessReadingFlowResponse response = resolver.resolve(
                processo,
                usuario,
                List.of(movimentacao),
                List.of(evento),
                modeProfile,
                presetProfile
        );

        assertEquals(5L, response.totalEntries());
        assertTrue(response.totalInlineActs() >= 3);
        assertEquals(1L, response.totalMovements());
        assertEquals(1L, response.totalEvents());
        assertFalse(response.entries().isEmpty());
        assertTrue(response.entries().stream().anyMatch(entry -> entry.sourceType().equals("PROCESS_INLINE_TEXT")));
        assertTrue(response.entries().stream().anyMatch(entry -> entry.sourceType().equals("MOVIMENTACAO_PROCESSUAL")));
        assertTrue(response.entries().stream().anyMatch(entry -> entry.sourceType().equals("EVENTO_PROCESSUAL")));
    }
}
