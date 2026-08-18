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
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.model.dto.AnexoDeclarado;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class CanalTipadoAjuizamentoIT extends PjbFlowItBase {

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
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void canalTipado_trabalhistaComTodosDocumentosTyped_routingRealAprovado_deveRetornar200() throws Exception {
        ProcessoRequest request = ProcessoRequest.builder()
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
                .valorCausa(new BigDecimal("12000.00"))
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
}
