package com.tcc.pjb.backend.controller.processual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.model.dto.AnexoDeclarado;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
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
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class MotorComposicaoPolosRitoReclassificacaoDiagnosticoIT extends PjbFlowItBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProcessoRepository processoRepository;

    @MockitoBean private NationalProceduralRoutingService nationalProceduralRoutingService;
    @MockitoBean private JudicialConnectorLifecycleService judicialConnectorLifecycleService;
    @MockitoBean private IAOrchestrator iaOrchestrator;

    @BeforeEach
    void setup() {
        usuarioRepository.save(novoAdvogado());
        when(judicialConnectorLifecycleService.submitAndSynchronize(any(), any(), any(), anyBoolean()))
                .thenReturn(Optional.empty());
        when(iaOrchestrator.processar(any())).thenReturn(IAResponse.builder()
                .origem("TEST").texto("stub").status(IAResponse.StatusIA.SUCESSO)
                .confianca(1.0).dataGeracao(Instant.parse("2026-06-01T12:00:00Z"))
                .build());
        when(nationalProceduralRoutingService.analyzeProcess(any(Processo.class), any(), any()))
                .thenAnswer(inv -> {
                    Processo p = inv.getArgument(0);
                    String rito = p.getRito() != null ? p.getRito().name() : "COMUM_ORDINARIO";
                    return passingRoutingReport(rito);
                });
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void comumOrdinario_comTextoRealistaDeCobranca_naoDeveSerReclassificadoSilenciosamente() throws Exception {
        ProcessoRequest req = baseRequestComTextoRealista()
                .materia("CIVIL")
                .rito("COMUM_ORDINARIO")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .anexosDeclarados(List.of(
                        anx("p.pdf", TipoDocumento.PETICAO_INICIAL),
                        anx("proc.pdf", TipoDocumento.PROCURACAO),
                        anx("id.pdf", TipoDocumento.DOCUMENTO_IDENTIDADE),
                        anx("end.pdf", TipoDocumento.COMPROVANTE_ENDERECO),
                        anx("prova.pdf", TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
                ))
                .build();

        long processoId = ajuizar(req);

        Processo processo = processoRepository.findById(processoId).orElseThrow();
        assertThat(processo.getRito())
                .as("Rito deve permanecer COMUM_ORDINARIO como enviado na requisição, sem reclassificação silenciosa")
                .isEqualTo(com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO);
    }

    private ProcessoRequest.ProcessoRequestBuilder baseRequestComTextoRealista() {
        return ProcessoRequest.builder()
                .classe("Ação de Cobrança")
                .assunto("Cobranca de valores decorrentes de contrato de prestacao de servicos inadimplido")
                .resumoFatico("O réu deixou de pagar as parcelas contratadas referentes à prestação de serviços contínua, "
                        + "acumulando débito vencido e não quitado apesar de notificação extrajudicial prévia")
                .objetoProcessual("Cobranca de debito contratual decorrente de inadimplemento de contrato de prestacao de servicos, "
                        + "com pedido de condenacao ao pagamento do valor principal acrescido de juros e correcao monetaria")
                .pedidoPrincipal("Condenação ao pagamento do débito contratual em aberto, acrescido de juros e correção monetária")
                .parteAutoraNome("Maria da Silva")
                .parteAutoraCpf("12345678909")
                .parteReuNome("Empresa Ré Ltda")
                .parteReuCpf("98765432100")
                .ufAutor("CE")
                .comarcaAutor("Fortaleza")
                .valorCausa(new BigDecimal("10000.00"));
    }

    private AnexoDeclarado anx(String nome, TipoDocumento tipo) {
        return new AnexoDeclarado(nome, tipo);
    }

    private long ajuizar(ProcessoRequest req) throws Exception {
        MockMultipartFile dados = new MockMultipartFile("dados", "dados.json", "application/json",
                objectMapper.writeValueAsBytes(req));

        var builder = multipart("/api/v1/processos/ajuizar").file(dados);
        List<AnexoDeclarado> decl = req.getAnexosDeclarados();
        if (decl != null) {
            for (AnexoDeclarado d : decl) {
                builder.file(new MockMultipartFile("anexos", d.nomeArquivo(), "application/pdf", pdfValido()));
            }
        }

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private byte[] pdfValido() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static ProceduralRoutingReport passingRoutingReport(String ritoSugerido) {
        return new ProceduralRoutingReport(
                Instant.now(), null, null, null, null,
                "ESTADUAL", ritoSugerido,
                null, null, null,
                "Fortaleza", "Fortaleza", "CE",
                null, null, null, null,
                false, false, false,
                0.9d, "LOW",
                List.of(), null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                Map.of()
        );
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
}
