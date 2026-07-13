package com.tcc.pjb.backend.controller.processual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaEventoComportamental;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class ProcessoCommandControllerIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @MockitoBean
    private JudicialConnectorLifecycleService judicialConnectorLifecycleService;

    @MockitoBean
    private IAOrchestrator iaOrchestrator;

    @MockitoBean
    private NationalProceduralRoutingService nationalProceduralRoutingService;

    @MockitoBean
    private CompletudeDocumentalPolicyService completudeDocumentalPolicyService;

    @BeforeEach
    void setup() {
        usuarioRepository.save(novoAdvogado());
        when(judicialConnectorLifecycleService.submitAndSynchronize(any(), any(), any(), anyBoolean())).thenReturn(Optional.empty());
        when(iaOrchestrator.processar(any())).thenReturn(IAResponse.builder()
                .origem("TEST")
                .texto("Resumo IA governado")
                .status(IAResponse.StatusIA.SUCESSO)
                .confianca(1.0)
                .dataGeracao(Instant.parse("2026-04-16T12:00:00Z"))
                .build());
        when(nationalProceduralRoutingService.analyzeProcess(any(), any(), any()))
                .thenAnswer(inv -> {
                    Processo p = inv.getArgument(0);
                    String rito = p.getRito() != null ? p.getRito().name() : "COMUM_ORDINARIO";
                    return passingRoutingReport(rito);
                });
        when(completudeDocumentalPolicyService.diagnosticar(any(), any()))
                .thenAnswer(inv -> new CompletudeDocumentalPolicyService.DiagnosticoCompletudeDocumental(
                        false, List.of(), inv.getArgument(0)));
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void deveAjuizarViaMultipartEPropagarJuizo100DigitalParaEfeitosPosCommit() throws Exception {
        ProcessoRequest request = novoRequestBase();
        request.setJuizo100Digital(true);
        MockMultipartFile dados = new MockMultipartFile("dados", "dados.json", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile anexo = new MockMultipartFile("anexos", "peticao-inicial.pdf", "application/pdf", pdfValido());

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados)
                        .file(anexo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.classe").value("Ação de cobrança"))
                .andExpect(jsonPath("$.materia").value("CIVIL"));

        Processo processo = awaitAtMost(
                "processo persistido via boundary HTTP",
                () -> processoRepository.findAll().stream().findFirst()
                        .map(p -> processoRepository.findProcessoCompletoById(p.getId()).orElse(null))
                        .orElse(null),
                value -> value != null && value.getId() != null && value.getUsuario() != null
        );

        assertThat(processo.getUsuario().getEmail()).isEqualTo("advogado@test.local");
        assertThat(processo.getTribunalCodigoRoteado()).isEqualTo("TJCE");
        assertThat(processo.getUf()).isEqualTo("CE");
        verify(judicialConnectorLifecycleService, timeout(5000)).submitAndSynchronize(any(), any(), any(), org.mockito.ArgumentMatchers.eq(true));

        AuditoriaEventoComportamental audit = awaitAtMost(
                "auditoria imutável do ajuizamento HTTP",
                () -> auditoriaRepository.search(String.valueOf(processo.getId()), "PROCESSO_AJUIZADO", null, PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .orElse(null),
                value -> value != null && value.getDetalhes() != null
        );

        assertThat(audit.getDetalhes()).contains("Juízo 100% Digital: true");
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void deveAceitarMultipartSemListaDeAnexos() throws Exception {
        ProcessoRequest request = novoRequestBase();
        MockMultipartFile dados = new MockMultipartFile("dados", "dados.json", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.classe").value("Ação de cobrança"));

        verify(judicialConnectorLifecycleService, timeout(5000)).submitAndSynchronize(any(), any(), any(), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void deveRejeitarAnexoNaoPdfNoBoundaryMultipartSemPersistirProcesso() throws Exception {
        long processosAntes = processoRepository.count();
        ProcessoRequest request = novoRequestBase();
        MockMultipartFile dados = new MockMultipartFile("dados", "dados.json", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile anexoInvalido = new MockMultipartFile("anexos", "peticao.txt", "text/plain", "conteudo invalido".getBytes());

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados)
                        .file(anexoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Formato não suportado")));

        assertThat(processoRepository.count()).isEqualTo(processosAntes);
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void deveRejeitarDadosInvalidosComFieldErrorsSemPersistirProcesso() throws Exception {
        long processosAntes = processoRepository.count();
        ProcessoRequest request = ProcessoRequest.builder()
                .assunto("Cobrança contratual")
                .build();
        MockMultipartFile dados = new MockMultipartFile("dados", "dados.json", "application/json", objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Dados inválidos."))
                .andExpect(jsonPath("$.fieldErrors.classe").value("Classe é obrigatória"))
                .andExpect(jsonPath("$.fieldErrors.materia").value("Matéria é obrigatória"))
                .andExpect(jsonPath("$.fieldErrors.rito").value("Rito é obrigatório"));

        assertThat(processoRepository.count()).isEqualTo(processosAntes);
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void devePersistirProcessoMesmoQuandoConectorJudicialFalhaNoPosCommit() throws Exception {
        ProcessoRequest request = novoRequestBase();
        request.setJuizo100Digital(true);
        MockMultipartFile dados = new MockMultipartFile("dados", "dados.json", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile anexo = new MockMultipartFile("anexos", "peticao-inicial.pdf", "application/pdf", pdfValido());
        doThrow(new RuntimeException("conector indisponível"))
                .when(judicialConnectorLifecycleService)
                .submitAndSynchronize(any(), any(), any(), anyBoolean());

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados)
                        .file(anexo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.classe").value("Ação de cobrança"));

        Processo processo = awaitAtMost(
                "processo persistido mesmo com falha no conector judicial",
                () -> processoRepository.findAll().stream().findFirst()
                        .map(p -> processoRepository.findProcessoCompletoById(p.getId()).orElse(null))
                        .orElse(null),
                value -> value != null && value.getId() != null
        );

        assertThat(processo.getUsuario()).isNotNull();
        assertThat(processo.getUsuario().getEmail()).isEqualTo("advogado@test.local");

        AuditoriaEventoComportamental audit = awaitAtMost(
                "auditoria imutável preservada após falha do conector judicial",
                () -> auditoriaRepository.search(String.valueOf(processo.getId()), "PROCESSO_AJUIZADO", null, PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .orElse(null),
                value -> value != null && value.getDetalhes() != null
        );

        assertThat(audit.getDetalhes()).contains("ConnectorSubmission: SKIPPED");
        verify(judicialConnectorLifecycleService, timeout(5000)).submitAndSynchronize(any(), any(), any(), org.mockito.ArgumentMatchers.eq(true));
    }

    private ProcessoRequest novoRequestBase() {
        return ProcessoRequest.builder()
                .classe("Ação de cobrança")
                .assunto("Cobrança contratual")
                .resumoFatico("Contrato inadimplido pelo réu")
                .objetoProcessual("Cobrança de dívida líquida")
                .pedidoPrincipal("Condenação ao pagamento da dívida")
                .pedidos(List.of("Condenação", "Juros legais"))
                .provas(List.of("Contrato", "Notificação extrajudicial"))
                .parteAutoraNome("Maria da Silva")
                .parteAutoraCpf("12345678909")
                .parteReuNome("Empresa Ré Ltda")
                .parteReuCpf("98765432100")
                .ufAutor("CE")
                .comarcaAutor("Fortaleza")
                .foroAutor("Fortaleza")
                .cidadeFato("Fortaleza")
                .municipioFato("Fortaleza")
                .foroPretendido("Fortaleza")
                .tipoAcao("Cobrança")
                .varaPretendida("2ª Vara Cível")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .materia("CIVIL")
                .rito("COMUM_ORDINARIO")
                .valorCausa(new BigDecimal("12000.00"))
                .build();
    }

    private Usuario novoAdvogado() {
        Usuario advogado = new Usuario();
        advogado.setNome("Advogado Teste");
        advogado.setEmail("advogado@test.local");
        advogado.setSenha("x");
        advogado.setCpf("12345678909");
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);
        advogado.setPerfil(TipoUsuario.ADVOGADO.name());
        advogado.setAtivo(true);
        return advogado;
    }

    private byte[] pdfValido() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private static ProceduralRoutingReport passingRoutingReport(String ritoSugerido) {
        return new ProceduralRoutingReport(
                Instant.now(), null, null, null, null,
                "ESTADUAL", ritoSugerido,
                "TJCE", null, null,
                "Fortaleza", "Fortaleza", "CE",
                null, null, null, null,
                false, false, false,
                0.9d, "LOW",
                List.of(), null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                Map.of()
        );
    }

    private static <T> T awaitAtMost(String description, Supplier<T> supplier, Predicate<T> predicate) {
        long timeoutNanos = Duration.ofSeconds(5).toNanos();
        long deadline = System.nanoTime() + timeoutNanos;
        T current = supplier.get();
        while (!predicate.test(current)) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Timeout aguardando: " + description + " | valor atual=" + current);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrompido aguardando: " + description, ex);
            }
            current = supplier.get();
        }
        return current;
    }
}
