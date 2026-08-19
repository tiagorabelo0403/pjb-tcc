package com.tcc.pjb.backend.core.servidor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorSolicitacaoService;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class FuncaoServidorDesignacaoInstitucionalIT extends PjbIntegrationTestBase {

    @Autowired
    private FuncaoServidorSolicitacaoService solicitacaoService;
    @Autowired
    private PjbAuthorizationService authorizationService;
    @Autowired
    private UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;
    @Autowired
    private UnidadeInstituicaoRepository unidadeInstituicaoRepository;
    @Autowired
    private InstituicaoRepository instituicaoRepository;
    @Autowired
    private LotacaoInstituicaoRepository lotacaoInstituicaoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String discriminador;
    private Long tribunalId;

    @BeforeEach
    void setUp() {
        discriminador = UUID.randomUUID().toString().substring(0, 8);
        tribunalId = jdbcTemplate.queryForObject("SELECT id FROM tb_tribunal WHERE sigla = 'TJCE'", Long.class);
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fluxoCompletoDoCaminhoBDesbloqueiaOGateQueAntesNegava() {
        Long servidorId = criarUsuario("SERVIDOR_FORUM");
        Long diretorId = criarUsuario("SERVIDOR_FORUM");
        String unidadeCodigo = "VARA-E2E-" + discriminador;
        Long unidadeId = criarUnidadeCompetencia(unidadeCodigo, null);

        jdbcTemplate.update(
                "INSERT INTO tb_funcao_servidor_judiciario (usuario_id, unidade_id, funcao, data_inicio, ativo) " +
                "VALUES (?, ?, 'DIRETOR_SECRETARIA', CURRENT_DATE, true)", diretorId, unidadeId);

        Processo processo = Processo.builder()
                .unidadeJudiciariaCodigo(unidadeCodigo)
                .numeroUnificado("0007777-" + discriminador + ".2026.8.06.0001")
                .build();

        autenticarComo(servidorId);
        String reqDenyId = "req-antes-" + discriminador;
        assertThatThrownBy(() -> RequestContext.run(reqDenyId, () ->
                authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.INTIMAR)))
                .isInstanceOf(AccessDeniedPjbException.class);

        var solicitacao = solicitacaoService.solicitar(servidorId, unidadeId, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, "Preciso intimar");
        var aprovada = solicitacaoService.aprovar(solicitacao.getId(), diretorId);
        assertThat(aprovada.getStatus().name()).isEqualTo("APROVADA");

        String reqAllowId = "req-depois-" + discriminador;
        RequestContext.run(reqAllowId, () ->
                authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.INTIMAR));
    }

    @Test
    void aprovacaoComPontePreenchidaMaterializaLotacaoInstituicaoConsultavel() {
        Long servidorId = criarUsuario("SERVIDOR_FORUM");
        Long diretorId = criarUsuario("SERVIDOR_FORUM");

        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.TRIBUNAL);
        instituicao.setNome("Instituicao E2E " + discriminador);
        instituicao = instituicaoRepository.save(instituicao);
        UnidadeInstituicao unidadeInstituicao = new UnidadeInstituicao();
        unidadeInstituicao.setInstituicao(instituicao);
        unidadeInstituicao.setNome("Secretaria E2E " + discriminador);
        unidadeInstituicao.setTipo(TipoUnidadeInstitucional.CARTORIO);
        unidadeInstituicao = unidadeInstituicaoRepository.save(unidadeInstituicao);

        String unidadeCodigo = "VARA-LOTACAO-" + discriminador;
        Long unidadeId = criarUnidadeCompetencia(unidadeCodigo, unidadeInstituicao.getId());

        jdbcTemplate.update(
                "INSERT INTO tb_funcao_servidor_judiciario (usuario_id, unidade_id, funcao, data_inicio, ativo) " +
                "VALUES (?, ?, 'DIRETOR_SECRETARIA', CURRENT_DATE, true)", diretorId, unidadeId);

        var solicitacao = solicitacaoService.solicitar(servidorId, unidadeId, FuncaoServidorJudiciario.OFICIAL_MAIOR, null);
        solicitacaoService.aprovar(solicitacao.getId(), diretorId);

        Usuario servidor = usuarioRepository.findById(servidorId).orElseThrow();
        List<LotacaoInstituicao> ativas = lotacaoInstituicaoRepository.findAtivasByUsuario(servidor);
        assertThat(ativas).hasSize(1);
        assertThat(ativas.get(0).getUnidade().getId()).isEqualTo(unidadeInstituicao.getId());
        assertThat(ativas.get(0).getPapelNaUnidade()).isEqualTo(FuncaoServidorJudiciario.OFICIAL_MAIOR.label());
    }

    private Long criarUsuario(String tipoUsuario) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, ?, 'hash', ?, true, ?) RETURNING id",
                Long.class, "Usr " + suffix, "usr." + suffix + "@pjb.test", tipoUsuario,
                String.format("%011d", Long.parseUnsignedLong(suffix, 16)), tipoUsuario);
    }

    private Long criarUnidadeCompetencia(String codigo, Long unidadeInstituicaoId) {
        if (unidadeInstituicaoId == null) {
            return jdbcTemplate.queryForObject(
                    "INSERT INTO tb_unidade_judiciaria_competencia (codigo, tribunal_id, tipo_vara) " +
                    "VALUES (?, ?, 'CIVEL_GERAL') RETURNING id", Long.class, codigo, tribunalId);
        }
        return jdbcTemplate.queryForObject(
                "INSERT INTO tb_unidade_judiciaria_competencia (codigo, tribunal_id, tipo_vara, unidade_instituicao_id) " +
                "VALUES (?, ?, 'CIVEL_GERAL', ?) RETURNING id", Long.class, codigo, tribunalId, unidadeInstituicaoId);
    }

    private void autenticarComo(Long usuarioId) {
        Jwt jwt = new Jwt(
                "token-" + usuarioId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("uid", String.valueOf(usuarioId)));
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_SERVIDOR"))));
    }
}
