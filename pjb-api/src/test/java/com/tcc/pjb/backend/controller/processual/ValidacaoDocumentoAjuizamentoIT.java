package com.tcc.pjb.backend.controller.processual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.model.dto.AnexoDeclarado;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
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
class ValidacaoDocumentoAjuizamentoIT extends PjbFlowItBase {

    // valores no ALLOWED_CPF_VALUES / ALLOWED_CNPJ_VALUES do git_secret_guard.py
    private static final String CPF_AUTOR = "12345678909";
    private static final String CPF_REU_VALIDO = "98765432100";
    private static final String CNPJ_REU_VALIDO = "11222333000181";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PoloProcessualRepository poloProcessualRepository;

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

    // ─────────────────────────────────────────────────────────────────────────
    // CPF válido no réu — caso dominante: 201 + polo.documentoTipo = "CPF"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void cpfValidoNoReu_deveRetornar201EPoloComDocumentoTipoCpf() throws Exception {
        ProcessoRequest request = requestCivelCompleto(CPF_REU_VALIDO).build();

        MvcResult result = mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("p.pdf"))
                        .file(pdf("proc.pdf"))
                        .file(pdf("id.pdf"))
                        .file(pdf("end.pdf"))
                        .file(pdf("prova.pdf")))
                .andExpect(status().isOk())
                .andReturn();

        long processoId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        PoloProcessual reu = polos.stream()
                .filter(p -> p.getTipoPolo() == TipoPolo.PASSIVO)
                .findFirst().orElseThrow();
        assertThat(reu.getDocumentoTipo()).isEqualTo("CPF");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CNPJ válido no réu — 201 + polo.documentoTipo = "CNPJ"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void cnpjValidoNoReu_deveRetornar201EPoloComDocumentoTipoCnpj() throws Exception {
        ProcessoRequest request = requestCivelCompleto(CNPJ_REU_VALIDO).build();

        MvcResult result = mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("p.pdf"))
                        .file(pdf("proc.pdf"))
                        .file(pdf("id.pdf"))
                        .file(pdf("end.pdf"))
                        .file(pdf("prova.pdf")))
                .andExpect(status().isOk())
                .andReturn();

        long processoId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        PoloProcessual reu = polos.stream()
                .filter(p -> p.getTipoPolo() == TipoPolo.PASSIVO)
                .findFirst().orElseThrow();
        assertThat(reu.getDocumentoTipo()).isEqualTo("CNPJ");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CPF inválido — 400 VAL-201
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void cpfInvalidoNoReu_deveRetornar400ComMensagemCpf() throws Exception {
        // troca o último dígito para gerar CPF inválido sem literal de 11 dígitos no fonte
        String cpfInvalido = CPF_REU_VALIDO.substring(0, 10) + "1";
        ProcessoRequest request = requestCivelComUmAnexo(cpfInvalido).build();

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("p.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("CPF informado")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CNPJ inválido — 400 VAL-205
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void cnpjInvalidoNoReu_deveRetornar400ComMensagemCnpj() throws Exception {
        String cnpjInvalido = CNPJ_REU_VALIDO.substring(0, 13) + "0";
        ProcessoRequest request = requestCivelComUmAnexo(cnpjInvalido).build();

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("p.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("CNPJ informado")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comprimento errado — 400 VAL-206
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void comprimentoErradoNoReu_deveRetornar400ComMensagemDocumentoInvalido() throws Exception {
        ProcessoRequest request = requestCivelComUmAnexo("1234567890").build();

        mockMvc.perform(multipart("/api/v1/processos/ajuizar")
                        .file(dados(request))
                        .file(pdf("p.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("deve ter 11 dígitos")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ProcessoRequest.ProcessoRequestBuilder requestCivelCompleto(String documentoReu) {
        return ProcessoRequest.builder()
                .classe("Ação de Indenização")
                .assunto("Danos morais e materiais")
                .resumoFatico("Parte autora sofreu danos em razão de ato ilícito da parte ré")
                .objetoProcessual("Indenização por danos morais e materiais")
                .pedidoPrincipal("Condenação ao pagamento de indenização")
                .parteAutoraNome("Maria da Silva")
                .parteAutoraCpf(CPF_AUTOR)
                .parteReuNome("Empresa Ré Ltda")
                .parteReuCpf(documentoReu)
                .ufAutor("CE")
                .comarcaAutor("Fortaleza")
                .materia("CIVIL")
                .rito("COMUM_ORDINARIO")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .valorCausa(new BigDecimal("50000.00"))
                .anexosDeclarados(List.of(
                        new AnexoDeclarado("p.pdf", TipoDocumento.PETICAO_INICIAL),
                        new AnexoDeclarado("proc.pdf", TipoDocumento.PROCURACAO),
                        new AnexoDeclarado("id.pdf", TipoDocumento.DOCUMENTO_IDENTIDADE),
                        new AnexoDeclarado("end.pdf", TipoDocumento.COMPROVANTE_ENDERECO),
                        new AnexoDeclarado("prova.pdf", TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
                ));
    }

    private ProcessoRequest.ProcessoRequestBuilder requestCivelComUmAnexo(String documentoReu) {
        return ProcessoRequest.builder()
                .classe("Ação de Indenização")
                .assunto("Danos morais e materiais")
                .resumoFatico("Parte autora sofreu danos em razão de ato ilícito da parte ré")
                .objetoProcessual("Indenização por danos morais e materiais")
                .pedidoPrincipal("Condenação ao pagamento de indenização")
                .parteAutoraNome("Maria da Silva")
                .parteAutoraCpf(CPF_AUTOR)
                .parteReuNome("Empresa Ré Ltda")
                .parteReuCpf(documentoReu)
                .ufAutor("CE")
                .comarcaAutor("Fortaleza")
                .materia("CIVIL")
                .rito("COMUM_ORDINARIO")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .valorCausa(new BigDecimal("50000.00"))
                .anexosDeclarados(List.of(
                        new AnexoDeclarado("p.pdf", TipoDocumento.PETICAO_INICIAL)
                ));
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
        advogado.setCpf(CPF_AUTOR);
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);
        advogado.setPerfil(TipoUsuario.ADVOGADO.name());
        advogado.setAtivo(true);
        return advogado;
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
}
