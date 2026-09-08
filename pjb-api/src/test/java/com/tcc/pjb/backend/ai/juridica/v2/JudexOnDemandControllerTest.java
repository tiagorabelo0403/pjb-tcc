package com.tcc.pjb.backend.ai.juridica.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.juridica.v2.dto.JudexGenerateMinutaRequest;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.ProcessoQueryRepository;
import com.tcc.pjb.backend.service.semantic.SemanticPrecedentSearchService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;

class JudexOnDemandControllerTest {

    private AiModelClient aiModelV2;
    private ObjectProvider<ProcessoQueryRepository> queryRepositoryProvider;
    private ObjectProvider<SemanticPrecedentSearchService> precedentSearchProvider;
    private SemanticPrecedentSearchService precedentSearch;
    private JudexOnDemandController controller;

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

        controller = new JudexOnDemandController(aiModelV2, queryRepositoryProvider, precedentSearchProvider);
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

        ResponseEntity<String> response = controller.gerarMinuta(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
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

        controller.gerarMinuta(request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelV2).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).doesNotContain("Precedentes relacionados");
    }

    @Test
    void gerarMinutaSemServicoDePrecedentesDisponivelMantemComportamentoAnterior() {
        when(precedentSearchProvider.getIfAvailable()).thenReturn(null);

        JudexGenerateMinutaRequest request = new JudexGenerateMinutaRequest(
                null, null, "triagem inicial", "texto da peticao");

        controller.gerarMinuta(request);

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

        ResponseEntity<String> response = controller.gerarMinuta(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("minuta gerada");
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
        controller.gerarMinuta(request);

        verify(precedentSearch).semanticSearch(
                (RamoDireito) org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("PROCEDIMENTO_COMUM_ORDINARIO"),
                anyString(),
                anyInt());
    }
}
