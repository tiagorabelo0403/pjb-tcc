package com.tcc.pjb.backend.ai.juridica.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.juridica.v2.dto.JudexGenerateMinutaRequest;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.ProcessoQueryRepository;
import com.tcc.pjb.backend.service.semantic.SemanticPrecedentSearchService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class JudexMinutaServiceTest {

    private AiModelClient aiModelV2;
    private ObjectProvider<ProcessoQueryRepository> queryRepositoryProvider;
    private ObjectProvider<SemanticPrecedentSearchService> precedentSearchProvider;
    private SemanticPrecedentSearchService precedentSearch;
    private JudexMinutaService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        aiModelV2 = mock(AiModelClient.class);
        when(aiModelV2.generate(anyString())).thenReturn("minuta gerada");

        queryRepositoryProvider = mock(ObjectProvider.class);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(null);

        precedentSearch = mock(SemanticPrecedentSearchService.class);
        precedentSearchProvider = mock(ObjectProvider.class);
        when(precedentSearchProvider.getIfAvailable()).thenReturn(precedentSearch);

        service = new JudexMinutaService(aiModelV2, queryRepositoryProvider, precedentSearchProvider);
    }

    @Test
    void gerarMinutaIncluiPrecedentesRecuperadosNoPromptSemInventarJurisprudencia() {
        Precedente precedente = new Precedente();
        precedente.setTitulo("Tema 1234/STJ");
        precedente.setTese("Prazo prescricional interrompido pela citação válida.");
        when(precedentSearch.semanticSearch(any(), nullable(String.class), anyString(), anyInt()))
                .thenReturn(List.of(precedente));

        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(
                null, "considere o prazo", "triagem inicial", "texto da peticao");

        String minuta = service.gerar(request);

        assertThat(minuta).isNotNull();
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelV2).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("Precedentes relacionados");
        assertThat(prompt).contains("Tema 1234/STJ");
        assertThat(prompt).contains("Prazo prescricional interrompido pela citação válida.");
        assertThat(prompt).contains("nunca cite jurisprudência que não esteja nesta lista");
    }

    @Test
    void gerarMinutaSemPrecedentesEncontradosNaoAlteraPrompt() {
        when(precedentSearch.semanticSearch(any(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(
                null, null, "triagem inicial", "texto da peticao");

        service.gerar(request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelV2).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).doesNotContain("Precedentes relacionados");
    }

    @Test
    void gerarMinutaSemServicoDePrecedentesDisponivelMantemComportamentoAnterior() {
        when(precedentSearchProvider.getIfAvailable()).thenReturn(null);

        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(
                null, null, "triagem inicial", "texto da peticao");

        service.gerar(request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelV2).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).doesNotContain("Precedentes relacionados");
    }

    @Test
    void gerarMinutaDegradaSemQuebrarQuandoBuscaDePrecedentesFalha() {
        when(precedentSearch.semanticSearch(any(), nullable(String.class), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Ollama indisponível"));

        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(
                null, null, "triagem inicial", "texto da peticao");

        String minuta = service.gerar(request);

        assertThat(minuta).isEqualTo("minuta gerada");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelV2).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).doesNotContain("Precedentes relacionados");
    }

    @Test
    void gerarMinutaUsaRitoProcessualDoProcessoQuandoBuscaPorProcessoId() {
        ProcessoQueryRepository repo = mock(ProcessoQueryRepository.class);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(repo);

        ProcessoQueryModel processo = new ProcessoQueryModel();
        processo.setAnaliseTriagemV1("triagem");
        processo.setPeticaoInicialText("peticao");
        processo.setRitoProcessual("PROCEDIMENTO_COMUM_ORDINARIO");
        when(repo.findById(42L)).thenReturn(Optional.of(processo));

        when(precedentSearch.semanticSearch(any(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(42L, null, null, null);
        service.gerar(request);

        verify(precedentSearch).semanticSearch(
                (RamoDireito) org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("PROCEDIMENTO_COMUM_ORDINARIO"),
                anyString(),
                anyInt());
    }

    @Test
    void processoInexistenteNoIndiceDeLeituraNaoVirarErroDeServidor() {
        ProcessoQueryRepository repo = mock(ProcessoQueryRepository.class);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(repo);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(99L, null, null, null);

        assertThatThrownBy(() -> service.gerar(request))
                .as("pedir minuta de processo inexistente e erro de quem pede, nao falha do servidor: "
                        + "RecursoNaoEncontradoException vira 404 no tratador da API, e RuntimeException "
                        + "crua virava 500")
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(aiModelV2, never()).generate(anyString());
    }

    @Test
    void semTextoNemProcessoNaoChamaOModelo() {
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(null);

        // ordem do record: processoId, promptAdicional, analiseV1, peticaoInicialText
        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(null, "so instrucoes", null, null);

        assertThatThrownBy(() -> service.gerar(request))
                .isInstanceOf(JudexMinutaService.DadosInsuficientesException.class)
                .hasMessageContaining("Dados insuficientes");

        verify(aiModelV2, never()).generate(anyString());
    }
}
