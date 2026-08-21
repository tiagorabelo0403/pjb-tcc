package com.tcc.pjb.backend.controller.processual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.atoordinatorio.AtoOrdinatorioRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.enums.TipoAtoOrdinatorio;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TribunalRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova end-to-end que a capacidade PROFERIR, ate aqui so existente no modelo, agora tem fluxo real:
 * servidor com podeProferir()=true consegue proferir e assinar um ato ordinatorio; servidor sem a
 * capacidade e barrado pelo gate real de PjbAuthorizationService.requireFuncaoServidorCapability.
 *
 * <p>Padrao de seeding (unidade judiciaria real + vinculo por codigo, nao por Processo.getId())
 * copiado verbatim de {@code PjbAuthorizationFuncaoServidorCapabilityIT}, o unico teste real
 * existente que ja prova esse gate contra Postgres — nao inventar um novo padrao de seeding.
 */
@AutoConfigureMockMvc
class AtoOrdinatorioServidorFlowIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    FuncaoServidorJudiciarioRepository funcaoServidorJudiciarioRepository;

    @Autowired
    TribunalRepository tribunalRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    MovimentacaoProcessualRepository movimentacaoProcessualRepository;

    @Autowired
    ObjectMapper objectMapper;

    private String seedUnidadeCodigo(String discriminador) {
        Long tribunalId = tribunalRepository.findBySigla("TJCE").orElseThrow(
                () -> new IllegalStateException("TJCE nao encontrado - verificar seed de tb_tribunal")).getId();
        String unidadeCodigo = "VARA-ATO-ORD-" + discriminador;
        jdbcTemplate.update(
                "INSERT INTO tb_unidade_judiciaria_competencia (codigo, tribunal_id, tipo_vara) VALUES (?, ?, 'CIVEL_GERAL')",
                unidadeCodigo, tribunalId);
        return unidadeCodigo;
    }

    @Test
    void diretorSecretariaComPodeProferirConsegueProferirAtoOrdinatorio() throws Exception {
        String discriminador = UUID.randomUUID().toString().substring(0, 8);
        String unidadeCodigo = seedUnidadeCodigo(discriminador);
        Long unidadeId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_unidade_judiciaria_competencia WHERE codigo = ?", Long.class, unidadeCodigo);

        Usuario servidor = new Usuario();
        servidor.setNome("Diretor Secretaria Teste");
        servidor.setEmail("diretor.proferir." + discriminador + "@pjb.local");
        servidor.setCpf(String.format("%011d", Long.parseUnsignedLong(discriminador, 16)));
        servidor.setAtivo(true);
        servidor.setTipoUsuario(TipoUsuario.SERVIDOR);
        servidor.setPerfil(TipoUsuario.SERVIDOR.name());
        servidor = usuarioRepository.save(servidor);
        Long servidorId = servidor.getId();

        funcaoServidorJudiciarioRepository.save(new FuncaoServidorJudiciarioEntity(
                servidorId, unidadeId, FuncaoServidorJudiciario.DIRETOR_SECRETARIA,
                LocalDate.now().minusDays(1), null, "Portaria " + discriminador));

        Processo processo = Processo.builder()
                .unidadeJudiciariaCodigo(unidadeCodigo)
                .numeroProcesso("0009999-" + discriminador + ".2026.8.06.0001")
                .faseAtual(FaseProcessual.INSTRUCAO)
                .build();
        processo = processoRepository.save(processo);

        AtoOrdinatorioRequest request = new AtoOrdinatorioRequest(
                processo.getId(), TipoAtoOrdinatorio.VISTA_PARTE_CONTRARIA, "manifeste-se em 5 dias");

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/processo/ato-ordinatorio")
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(servidorId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_SERVIDOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(201);

        List<DocumentoProcessual> documentos = documentoProcessualRepository.findByProcessoId(processo.getId());
        assertThat(documentos).hasSize(1);

        List<MovimentacaoProcessual> movimentacoes =
                movimentacaoProcessualRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId());
        assertThat(movimentacoes).hasSize(1);
        MovimentacaoProcessual movimentacao = movimentacoes.get(0);
        assertThat(movimentacao.getFaseDe()).isEqualTo(FaseProcessual.INSTRUCAO);
        assertThat(movimentacao.getFasePara()).isEqualTo(FaseProcessual.INSTRUCAO);
    }

    @Test
    void servidorSemPodeProferirRecebeErroDeAutorizacao() throws Exception {
        String discriminador = UUID.randomUUID().toString().substring(0, 8);
        String unidadeCodigo = seedUnidadeCodigo(discriminador);
        Long unidadeId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_unidade_judiciaria_competencia WHERE codigo = ?", Long.class, unidadeCodigo);

        Usuario servidor = new Usuario();
        servidor.setNome("Tecnico Sem Capacidade");
        servidor.setEmail("tecnico.sem.proferir." + discriminador + "@pjb.local");
        servidor.setCpf(String.format("%011d", Long.parseUnsignedLong(discriminador, 16)));
        servidor.setAtivo(true);
        servidor.setTipoUsuario(TipoUsuario.SERVIDOR);
        servidor.setPerfil(TipoUsuario.SERVIDOR.name());
        servidor = usuarioRepository.save(servidor);
        Long servidorId = servidor.getId();

        funcaoServidorJudiciarioRepository.save(new FuncaoServidorJudiciarioEntity(
                servidorId, unidadeId, FuncaoServidorJudiciario.TECNICO_JUDICIARIO,
                LocalDate.now().minusDays(1), null, "Portaria " + discriminador));

        Processo processo = Processo.builder()
                .unidadeJudiciariaCodigo(unidadeCodigo)
                .numeroProcesso("0008888-" + discriminador + ".2026.8.06.0001")
                .faseAtual(FaseProcessual.INSTRUCAO)
                .build();
        processo = processoRepository.save(processo);

        AtoOrdinatorioRequest request = new AtoOrdinatorioRequest(
                processo.getId(), TipoAtoOrdinatorio.VISTA_PARTE_CONTRARIA, null);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/processo/ato-ordinatorio")
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(servidorId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_SERVIDOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isNotEqualTo(201);
        assertThat(documentoProcessualRepository.countByProcesso_Id(processo.getId())).isZero();
    }
}
