package com.tcc.pjb.backend.service.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PublicProcessoConsultaServiceTest {

    private static final String NUMERO = "0001234-56.2026.8.06.0001";

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
    private final PublicProcessoConsultaService service = new PublicProcessoConsultaService(processoRepository,
            movimentacaoRepository, mock(DocumentoProcessualRepository.class), mock(DocumentoPaginaRepository.class));

    @Test
    void consultaPublicaPorNumeroNaoExpoeIdDeBancoDoProcessoNemDasMovimentacoes() throws Exception {
        Processo processo = new Processo();
        processo.setId(9001L);
        processo.setNumeroUnificado(NUMERO);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        when(processoRepository.findByNumeroUnificado(NUMERO)).thenReturn(Optional.of(processo));
        MovimentacaoProcessual movimentacao = mock(MovimentacaoProcessual.class);
        when(movimentacao.getId()).thenReturn(7777L);
        when(movimentacao.getDataMovimentacao()).thenReturn(Instant.parse("2026-10-01T12:00:00Z"));
        when(movimentacao.getDescricao()).thenReturn("Processo distribuído");
        when(movimentacaoRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(9001L)).thenReturn(List.of(movimentacao));

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        JsonNode json = mapper.readTree(mapper.writeValueAsString(service.consultarPorNumero(NUMERO)));

        assertThat(json.get("numero").asText()).isEqualTo(NUMERO);
        assertThat(json.has("processoId")).isFalse();
        assertThat(json.get("movimentacoes").get(0).get("descricao").asText()).isEqualTo("Processo distribuído");
        assertThat(json.get("movimentacoes").get(0).get("id").isNull()).isTrue();
        assertThat(json.toString()).doesNotContain("9001").doesNotContain("7777");
    }
}
