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
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
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
class MotorComposicaoPolosAjuizamentoIT extends PjbFlowItBase {

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
    // 1. Cível — AUTOR + REU
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void civel_deveMaterilaizarAutorEReu() throws Exception {
        ProcessoRequest req = baseRequest()
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

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(2);
        assertPolo(polos, TipoPolo.ATIVO,   TipoParte.AUTOR);
        assertPolo(polos, TipoPolo.PASSIVO, TipoParte.REU);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Penal — ACUSACAO + ACUSADO
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void penal_deveMaterializarAcusacaoEAcusado() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("PENAL")
                .rito("PROCEDIMENTO_PENAL_COMUM")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .anexosDeclarados(List.of(
                        anx("p.pdf", TipoDocumento.PETICAO_INICIAL)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(2);
        assertPolo(polos, TipoPolo.ATIVO,   TipoParte.ACUSACAO);
        assertPolo(polos, TipoPolo.PASSIVO, TipoParte.ACUSADO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Trabalhista — RECLAMANTE + RECLAMADA
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void trabalhista_deveMaterializarReclamanteEReclamada() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("TRABALHISTA")
                .rito("TRABALHISTA_ORDINARIO")
                .tipoJusticaPretendida("TRABALHO")
                .tribunalPretendido("TRT7")
                .valorCausa(new BigDecimal("15000.00"))
                .anexosDeclarados(List.of(
                        anx("p.pdf",  TipoDocumento.PETICAO_INICIAL),
                        anx("pr.pdf", TipoDocumento.PROCURACAO),
                        anx("ct.pdf", TipoDocumento.CTPS),
                        anx("ca.pdf", TipoDocumento.CALCULO_INICIAL)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(2);
        assertPolo(polos, TipoPolo.ATIVO,   TipoParte.RECLAMANTE);
        assertPolo(polos, TipoPolo.PASSIVO, TipoParte.RECLAMADA);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Mandado de Segurança — IMPETRANTE + IMPETRADO
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void mandadoSeguranca_deveMaterializarImpetrante() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("ADMINISTRATIVO")
                .rito("ESPECIAL_MANDADO_SEGURANCA")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .anexosDeclarados(List.of(
                        anx("p.pdf",  TipoDocumento.PETICAO_INICIAL),
                        anx("pr.pdf", TipoDocumento.PROCURACAO),
                        anx("prc.pdf", TipoDocumento.PROVA_PRE_CONSTITUIDA),
                        anx("ato.pdf", TipoDocumento.ATO_COATOR),
                        anx("doc.pdf", TipoDocumento.DOCUMENTO_AUTORIDADE_COATORA)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(2);
        assertPolo(polos, TipoPolo.ATIVO,   TipoParte.IMPETRANTE);
        assertPolo(polos, TipoPolo.PASSIVO, TipoParte.IMPETRADO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Previdenciário — apenas SEGURADO (INSS é external=false, abstém)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void previdenciario_deveMaterializarApenasSegurado() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("PREVIDENCIARIO")
                .rito("PREVIDENCIARIO_COMUM")
                .tipoJusticaPretendida("FEDERAL")
                .tribunalPretendido("TRF5")
                .parteReuNome(null)
                .parteReuCpf(null)
                .anexosDeclarados(List.of(
                        anx("p.pdf",   TipoDocumento.PETICAO_INICIAL),
                        anx("pr.pdf",  TipoDocumento.PROCURACAO),
                        anx("id.pdf",  TipoDocumento.DOCUMENTO_IDENTIDADE),
                        anx("cn.pdf",  TipoDocumento.CNIS),
                        anx("req.pdf", TipoDocumento.REQUERIMENTO_ADMINISTRATIVO)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(1);
        assertPolo(polos, TipoPolo.ATIVO, TipoParte.SEGURADO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Militar IPM — apenas INVESTIGADO (AUTORIDADE_MILITAR é external=false)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void militarIpm_deveMaterializarApenasInvestigado() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("MILITAR")
                .rito("MILITAR_IPM")
                .tipoJusticaPretendida("MILITAR")
                .tribunalPretendido("STM")
                .parteAutoraNome(null)
                .parteAutoraCpf(null)
                .anexosDeclarados(List.of(
                        anx("p.pdf",  TipoDocumento.PETICAO_INICIAL),
                        anx("po.pdf", TipoDocumento.PORTARIA_INSTAURACAO),
                        anx("da.pdf", TipoDocumento.DESIGNACAO_AUTORIDADE),
                        anx("pi.pdf", TipoDocumento.PECAS_IPM)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(1);
        assertPolo(polos, TipoPolo.PASSIVO, TipoParte.INVESTIGADO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Habeas Corpus — IMPETRANTE materializa; PACIENTE abstém (não mapeado)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void habeasCorpus_deveMaterializarApenasImpetrante_pacienteAbstem() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("PENAL")
                .rito("ESPECIAL_HABEAS_CORPUS")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .parteReuNome(null)
                .parteReuCpf(null)
                .anexosDeclarados(List.of(
                        anx("p.pdf",  TipoDocumento.PETICAO_INICIAL),
                        anx("pr.pdf", TipoDocumento.PECAS_RELEVANTES),
                        anx("il.pdf", TipoDocumento.PROVA_ILEGALIDADE)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(1);
        assertPolo(polos, TipoPolo.ATIVO, TipoParte.IMPETRANTE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. Trabalhista Inquérito Falta Grave — EMPREGADOR como ATIVO (override)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void inqueritofaltaGrave_empregadorDeveSerAtivo_override() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("TRABALHISTA")
                .rito("TRABALHISTA_INQUERITO_FALTA_GRAVE")
                .tipoJusticaPretendida("TRABALHO")
                .tribunalPretendido("TRT7")
                .valorCausa(new BigDecimal("10000.00"))
                .parteAutoraNome("Empresa Peticionante Ltda")
                .parteAutoraCpf("12345678909")
                .parteReuNome("João Empregado Estável")
                .parteReuCpf("98765432100")
                .anexosDeclarados(List.of(
                        anx("p.pdf",  TipoDocumento.PETICAO_INICIAL),
                        anx("de.pdf", TipoDocumento.DOCUMENTO_ESTABILIDADE),
                        anx("as.pdf", TipoDocumento.ATO_SUSPENSAO),
                        anx("pf.pdf", TipoDocumento.PROVA_FALTA_GRAVE),
                        anx("cp.pdf", TipoDocumento.CONTROLE_PRAZO_DECADENCIAL)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(2);
        assertPolo(polos, TipoPolo.ATIVO,   TipoParte.EMPREGADOR);
        assertPolo(polos, TipoPolo.PASSIVO, TipoParte.EMPREGADO_ESTAVEL);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. Trabalhista Ação Cumprimento — EMPREGADOR como PASSIVO (outro override)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void acaoCumprimento_empregadorDeveSerPassivo_override() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("TRABALHISTA")
                .rito("TRABALHISTA_ACAO_CUMPRIMENTO")
                .tipoJusticaPretendida("TRABALHO")
                .tribunalPretendido("TRT7")
                .valorCausa(new BigDecimal("8000.00"))
                .parteAutoraNome("Sindicato Substituto")
                .parteAutoraCpf("12345678909")
                .parteReuNome("Empresa Executada Ltda")
                .parteReuCpf("98765432100")
                .anexosDeclarados(List.of(
                        anx("p.pdf",  TipoDocumento.PETICAO_INICIAL),
                        anx("ic.pdf", TipoDocumento.INSTRUMENTO_COLETIVO),
                        anx("cd.pdf", TipoDocumento.CLAUSULA_DESCUMPRIDA),
                        anx("pd.pdf", TipoDocumento.PROVAS_DESCUMPRIMENTO)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(2);
        assertPolo(polos, TipoPolo.ATIVO,   TipoParte.SUBSTITUTO_PROCESSUAL);
        assertPolo(polos, TipoPolo.PASSIVO, TipoParte.EMPREGADOR);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. Propagação de CPF e território — polo ATIVO recebe CPF e UF da parte autora
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = "ROLE_ADVOGADO")
    void civel_devePropagar_cpf_e_territorio_corretamente() throws Exception {
        ProcessoRequest req = baseRequest()
                .materia("CIVIL")
                .rito("COMUM_ORDINARIO")
                .tipoJusticaPretendida("ESTADUAL")
                .tribunalPretendido("TJCE")
                .parteAutoraNome("Ana Autora da Silva")
                .parteAutoraCpf("12345678909")
                .parteReuNome("Empresa Ré Ltda")
                .parteReuCpf("98765432100")
                .ufAutor("CE")
                .comarcaAutor("Fortaleza")
                .ufReu("CE")
                .comarcaReu("Caucaia")
                .anexosDeclarados(List.of(
                        anx("p.pdf",  TipoDocumento.PETICAO_INICIAL),
                        anx("pr.pdf", TipoDocumento.PROCURACAO),
                        anx("id.pdf", TipoDocumento.DOCUMENTO_IDENTIDADE),
                        anx("en.pdf", TipoDocumento.COMPROVANTE_ENDERECO),
                        anx("pv.pdf", TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
                ))
                .build();

        long processoId = ajuizar(req);

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processoId, true);
        assertThat(polos).hasSize(2);

        PoloProcessual autor = polos.stream().filter(p -> p.getTipoPolo() == TipoPolo.ATIVO).findFirst().orElseThrow();
        assertThat(autor.getTipoParte()).isEqualTo(TipoParte.AUTOR);
        assertThat(autor.getNomeCompleto()).isEqualTo("Ana Autora Da Silva");
        assertThat(autor.getDocumento()).isEqualTo("12345678909");
        assertThat(autor.getUfDomicilio()).isEqualTo("CE");
        assertThat(autor.getComarcaDomicilio()).isEqualTo("Fortaleza");

        PoloProcessual reu = polos.stream().filter(p -> p.getTipoPolo() == TipoPolo.PASSIVO).findFirst().orElseThrow();
        assertThat(reu.getTipoParte()).isEqualTo(TipoParte.REU);
        assertThat(reu.getNomeCompleto()).isEqualTo("Empresa Ré Ltda");
        assertThat(reu.getDocumento()).isEqualTo("98765432100");
        assertThat(reu.getUfDomicilio()).isEqualTo("CE");
        assertThat(reu.getComarcaDomicilio()).isEqualTo("Caucaia");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ProcessoRequest.ProcessoRequestBuilder baseRequest() {
        return ProcessoRequest.builder()
                .classe("Ação")
                .assunto("Assunto processual")
                .resumoFatico("Fatos relevantes do caso")
                .objetoProcessual("Objeto do processo")
                .pedidoPrincipal("Pedido principal")
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

    private void assertPolo(List<PoloProcessual> polos, TipoPolo tipoPolo, TipoParte tipoParte) {
        assertThat(polos)
                .as("Deve existir polo %s com tipoParte %s", tipoPolo, tipoParte)
                .anyMatch(p -> p.getTipoPolo() == tipoPolo && p.getTipoParte() == tipoParte);
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
