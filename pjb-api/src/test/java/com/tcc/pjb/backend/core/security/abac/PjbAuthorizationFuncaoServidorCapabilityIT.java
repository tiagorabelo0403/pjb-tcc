package com.tcc.pjb.backend.core.security.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import com.tcc.pjb.backend.model.repository.TribunalRepository;
import java.time.LocalDate;
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
class PjbAuthorizationFuncaoServidorCapabilityIT extends PjbIntegrationTestBase {

    @Autowired
    private PjbAuthorizationService authorizationService;
    @Autowired
    private FuncaoServidorJudiciarioRepository funcaoServidorRepository;
    @Autowired
    private TribunalRepository tribunalRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String discriminador;
    private Long usuarioId;
    private Long unidadeIdCriada;
    private String unidadeCodigo;

    @BeforeEach
    void setUp() {
        discriminador = UUID.randomUUID().toString().substring(0, 8);
        Long tribunalId = tribunalRepository.findBySigla("TJCE").orElseThrow(
                () -> new IllegalStateException("TJCE nao encontrado - verificar seed de tb_tribunal")).getId();

        usuarioId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'SERVIDOR', 'hash', ?, true, 'SERVIDOR') RETURNING id",
                Long.class, "Srv Cap " + discriminador, "srv.cap." + discriminador + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(discriminador, 16)));

        unidadeCodigo = "VARA-CAP-" + discriminador;
        unidadeIdCriada = jdbcTemplate.queryForObject(
                "INSERT INTO tb_unidade_judiciaria_competencia (codigo, tribunal_id, tipo_vara) " +
                "VALUES (?, ?, 'CIVEL_GERAL') RETURNING id",
                Long.class, unidadeCodigo, tribunalId);
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void servidorComFuncaoAtivaECapacidadeNaoLancaEGravaTrilhaDeAuditoria() {
        funcaoServidorRepository.save(new FuncaoServidorJudiciarioEntity(
                usuarioId, unidadeIdCriada, FuncaoServidorJudiciario.DIRETOR_SECRETARIA,
                LocalDate.now().minusDays(30), null, "Portaria " + discriminador));
        autenticarComo(usuarioId);

        String numeroProcesso = "0001234-" + discriminador + ".2026.8.06.0001";
        Processo processo = Processo.builder()
                .unidadeJudiciariaCodigo(unidadeCodigo)
                .numeroUnificado(numeroProcesso)
                .build();

        String requestId = "req-capability-" + discriminador;
        RequestContext.run(requestId, () ->
                authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.CONCLUIR));

        Long trilhas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_authz_trail_read_model WHERE action = ? AND actor_id = ? AND allowed = true",
                Long.class, "FUNCAO_SERVIDOR_CAPABILITY", usuarioId);
        assertThat(trilhas).isEqualTo(1L);
    }

    @Test
    void servidorComFuncaoAtivaSemCapacidadeLancaAccessDenied() {
        funcaoServidorRepository.save(new FuncaoServidorJudiciarioEntity(
                usuarioId, unidadeIdCriada, FuncaoServidorJudiciario.ASSISTENTE_JUDICIARIO,
                LocalDate.now().minusDays(30), null, "Portaria " + discriminador));
        autenticarComo(usuarioId);

        Processo processo = Processo.builder()
                .unidadeJudiciariaCodigo(unidadeCodigo)
                .numeroUnificado("0009999-" + discriminador + ".2026.8.06.0001")
                .build();

        String requestId = "req-negado-" + discriminador;
        assertThatThrownBy(() -> RequestContext.run(requestId, () ->
                authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.CONCLUIR)))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void unidadeJudiciariaCodigoSemCorrespondenciaLancaAccessDenied() {
        autenticarComo(usuarioId);

        Processo processo = Processo.builder()
                .unidadeJudiciariaCodigo("CODIGO-INEXISTENTE-" + discriminador)
                .numeroUnificado("0005555-" + discriminador + ".2026.8.06.0001")
                .build();

        String requestId = "req-sem-unidade-" + discriminador;
        assertThatThrownBy(() -> RequestContext.run(requestId, () ->
                authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.CONCLUIR)))
                .isInstanceOf(AccessDeniedPjbException.class);
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
