package com.tcc.pjb.backend.controller.processual;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.model.dto.AnexoDeclarado;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class CompletudeDocumentalAjuizamentoIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JudicialConnectorLifecycleService judicialConnectorLifecycleService;

    @MockitoBean
    private IAOrchestrator iaOrchestrator;

    @MockitoBean
    private NationalProceduralRoutingService nationalProceduralRoutingService;

    @MockitoBean
    private ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;

    @MockitoBean
    private ProceduralConnectorExecutionService proceduralConnectorExecutionService;

    @BeforeEach
    void setup() {
        usuarioRepository.save(novoAdvogado());

        when(judicialConnectorLifecycleService.submitAndSynchronize(any(), any(), any(), anyBoolean()))
                .thenReturn(Optional.empty());
        when(iaOrchestrator.processar(any())).thenReturn(IAResponse.builder()
                .origem("TEST")
                .texto("IA stub")
                .status(IAResponse.StatusIA.SUCESSO)
                .confianca(1.0)
                .dataGeracao(Instant.parse("2026-06-01T12:00:00Z"))
                .build());

        // Routing mock: gate passes (forumAllocation=null, blockingIssues=[]).
        // Retorna rito canônico do processo para applyRoutingSnapshot preservá-lo.
        // ufSugerida/cidadeSugerida alimentam processo.uf/comarca via consolidate() (linha 82),
        // garantindo que TerritorialProcessualService não bloqueie por base territorial vazia.
        when(nationalProceduralRoutingService.analyzeProcess(any(Processo.class), any()))
                .thenAnswer(inv -> {
                    Processo p = inv.getArgument(0);
                    String ritoSugerido = p.getRito() != null ? p.getRito().name() : "TRABALHISTA_ORDINARIO";
                    return passingRoutingReport(ritoSugerido);
                });

        when(proceduralSubmissionBlueprintService.analyzeProcess(any(Processo.class), any()))
                .thenReturn(passingBlueprintReport());

        when(proceduralConnectorExecutionService.analyzeProcess(any(Processo.class), any(), any()))
                .thenReturn(passingConnectorReport());
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void cenarioA_trabalhistaComDocumentosCompletos_deveRetornar200() throws Exception {
        ProcessoRequest request = novoRequestTrabalhista()
                .anexosDeclarados(List.of(
                        new AnexoDeclarado("peticao.pdf", TipoDocumento.PETICAO_INICIAL),
                        new AnexoDeclarado("procuracao.pdf", TipoDocumento.PROCURACAO),
                        new AnexoDeclarado("ctps.pdf", TipoDocumento.CTPS),
                        new AnexoDeclarado("calculo.pdf", TipoDocumento.CALCULO_INICIAL)
                ))
                .build();

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("peticao.pdf"))
                        .file(pdf("procuracao.pdf"))
                        .file(pdf("ctps.pdf"))
                        .file(pdf("calculo.pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void cenarioB_trabalhistaSemCtps_deveRetornar400PorIncompletudeDocumental() throws Exception {
        ProcessoRequest request = novoRequestTrabalhista()
                .anexosDeclarados(List.of(
                        new AnexoDeclarado("peticao.pdf", TipoDocumento.PETICAO_INICIAL),
                        new AnexoDeclarado("procuracao.pdf", TipoDocumento.PROCURACAO),
                        new AnexoDeclarado("calculo.pdf", TipoDocumento.CALCULO_INICIAL)
                ))
                .build();

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("peticao.pdf"))
                        .file(pdf("procuracao.pdf"))
                        .file(pdf("calculo.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("incompletude documental")));
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void cenarioC_previdenciarioSemCnis_deveRetornar400PorIncompletudeDocumental() throws Exception {
        ProcessoRequest request = novoRequestPrevidenciario()
                .anexosDeclarados(List.of(
                        new AnexoDeclarado("peticao.pdf", TipoDocumento.PETICAO_INICIAL),
                        new AnexoDeclarado("procuracao.pdf", TipoDocumento.PROCURACAO),
                        new AnexoDeclarado("identidade.pdf", TipoDocumento.DOCUMENTO_IDENTIDADE),
                        new AnexoDeclarado("requerimento.pdf", TipoDocumento.REQUERIMENTO_ADMINISTRATIVO)
                ))
                .build();

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("peticao.pdf"))
                        .file(pdf("procuracao.pdf"))
                        .file(pdf("identidade.pdf"))
                        .file(pdf("requerimento.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("incompletude documental")));
    }

    private ProcessoRequest.ProcessoRequestBuilder novoRequestTrabalhista() {
        return ProcessoRequest.builder()
                .classe("Reclamação Trabalhista")
                .assunto("Rescisão contratual indireta e verbas rescisórias")
                .resumoFatico("Empregador descumpriu obrigações contratuais trabalhistas")
                .objetoProcessual("Pagamento de verbas rescisórias")
                .pedidoPrincipal("Reconhecimento da rescisão indireta")
                .pedidos(List.of("Aviso prévio indenizado", "FGTS com multa de 40%"))
                .provas(List.of("CTPS anotada", "Contracheques"))
                .parteAutoraNome("João Trabalhador")
                .parteAutoraCpf("12345678909")
                .parteReuNome("Empresa Empregadora Ltda")
                .ufAutor("SP")
                .comarcaAutor("São Paulo")
                .foroAutor("São Paulo")
                .cidadeFato("São Paulo")
                .municipioFato("São Paulo")
                .foroPretendido("São Paulo")
                .tipoAcao("Reclamação Trabalhista")
                .varaPretendida("1ª Vara do Trabalho")
                .tipoJusticaPretendida("TRABALHO")
                .tribunalPretendido("TRT2")
                .materia("TRABALHISTA")
                .rito("TRABALHISTA_ORDINARIO")
                .valorCausa(new BigDecimal("12000.00"));
    }

    private ProcessoRequest.ProcessoRequestBuilder novoRequestPrevidenciario() {
        return ProcessoRequest.builder()
                .classe("Concessão de Aposentadoria por Incapacidade Permanente")
                .assunto("Concessão de benefício previdenciário negado administrativamente")
                .resumoFatico("Segurado teve benefício indeferido pelo INSS sem justa causa")
                .objetoProcessual("Concessão de aposentadoria por incapacidade")
                .pedidoPrincipal("Condenação do INSS a conceder o benefício")
                .pedidos(List.of("Concessão do benefício", "Pagamento das parcelas atrasadas"))
                .provas(List.of("CNIS do segurado", "Requerimento administrativo"))
                .parteAutoraNome("Maria Segurada")
                .parteAutoraCpf("12345678909")
                .parteReuNome("Instituto Nacional do Seguro Social")
                .ufAutor("SP")
                .comarcaAutor("São Paulo")
                .foroAutor("São Paulo")
                .cidadeFato("São Paulo")
                .municipioFato("São Paulo")
                .foroPretendido("São Paulo")
                .tipoAcao("Concessão de Benefício Previdenciário")
                .varaPretendida("1ª Vara Federal")
                .tipoJusticaPretendida("FEDERAL")
                .tribunalPretendido("TRF3")
                .materia("PREVIDENCIARIO")
                .rito("PREVIDENCIARIO_JEF")
                .valorCausa(new BigDecimal("36000.00"));
    }

    private MockMultipartFile dados(ProcessoRequest request) throws Exception {
        return new MockMultipartFile("dados", "dados.json", "application/json",
                objectMapper.writeValueAsBytes(request));
    }

    private MockMultipartFile pdf(String filename) throws Exception {
        return new MockMultipartFile("anexos", filename, "application/pdf", pdfValido());
    }

    private byte[] pdfValido() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
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

    private static ProceduralRoutingReport passingRoutingReport(String ritoSugerido) {
        return new ProceduralRoutingReport(
                Instant.now(),
                "RECLAMACAO_TRABALHISTA",
                "TRABALHISTA",
                null,
                null,
                "TRABALHO",
                ritoSugerido,
                null,
                null,
                null,
                "São Paulo",
                "São Paulo",
                "SP",
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                0.9d,
                "LOW",
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
    }

    private static ProceduralSubmissionBlueprintReport passingBlueprintReport() {
        return new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                null,
                "ASSISTED",
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                false,
                false,
                false,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of()
        );
    }

    private static ProceduralConnectorExecutionReport passingConnectorReport() {
        return new ProceduralConnectorExecutionReport(
                Instant.now(),
                "ASSISTED",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
