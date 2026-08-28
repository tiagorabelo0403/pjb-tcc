package com.tcc.pjb.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prova que a V350 filtra de verdade: tabelas com usuario_id (own-row, backstop sobre
 * autoatendimento) e tb_marketplace_access_token (sem dono individual, system+admin only), sob
 * role NOSUPERUSER NOBYPASSRLS. pjb_icp_trust_anchor foi deliberadamente excluida da V350 — ver
 * comentario na propria migration.
 */
class PjbCredenciaisSessoesAtoresRlsIT extends PjbIntegrationTestBase {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void tabelasComUsuarioIdSaoOwnerScope_semDonoSaoSystemAdminOnly() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_cred_sess_probe_" + sufixo;

        Usuario donoA = usuarioRepository.save(novoUsuario("A", sufixo, "55566677701"));
        Usuario donoB = usuarioRepository.save(novoUsuario("B", sufixo, "55566677702"));
        Long processoA = processoRepository.save(baseProcesso("PROC-CRED-" + sufixo).usuario(donoA).build()).getId();

        Long credA = inserirCredencial(donoA.getId(), sufixo);
        Long credB = inserirCredencial(donoB.getId(), sufixo);
        Long passkeyA = inserirPasskey(donoA.getId(), sufixo);
        Long passkeyB = inserirPasskey(donoB.getId(), sufixo);
        Long stepupA = inserirDecisionStepup(donoA.getId(), processoA, sufixo);
        Long stepupB = inserirDecisionStepup(donoB.getId(), processoA, sufixo);
        UUID govbrLinkA = inserirGovBrLink(donoA.getId(), sufixo);
        UUID govbrLinkB = inserirGovBrLink(donoB.getId(), sufixo);
        Long clientAppId = inserirMarketplaceClientApp(sufixo);
        Long tokenId = inserirMarketplaceToken(clientAppId, sufixo);
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON operational_function_credentials, passkey_sessions, "
                + "tb_decision_stepup_consumption, tb_govbr_link_state, tb_marketplace_access_token TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);

        exec("SET LOCAL ROLE " + role);

        // 1) Sem ator definido: permissivo em todas (jobs/boot/migrations nao quebram).
        assertThat(contarPorId("operational_function_credentials", "id", String.valueOf(credA))).isEqualTo(1L);
        assertThat(contarPorId("operational_function_credentials", "id", String.valueOf(credB))).isEqualTo(1L);
        assertThat(contarPorId("tb_marketplace_access_token", "id", String.valueOf(tokenId))).isEqualTo(1L);

        // 2) Ator A: ve as proprias linhas de credencial/passkey/stepup/govbr, nao as de B.
        setActor(donoA.getId());
        assertThat(contarPorId("operational_function_credentials", "id", String.valueOf(credA))).isEqualTo(1L);
        assertThat(contarPorId("operational_function_credentials", "id", String.valueOf(credB)))
                .as("banco nega a credencial de outro usuario mesmo com WHERE explicito")
                .isZero();
        assertThat(contarPorId("passkey_sessions", "id", String.valueOf(passkeyA))).isEqualTo(1L);
        assertThat(contarPorId("passkey_sessions", "id", String.valueOf(passkeyB))).isZero();
        assertThat(contarPorId("tb_decision_stepup_consumption", "id", String.valueOf(stepupA))).isEqualTo(1L);
        assertThat(contarPorId("tb_decision_stepup_consumption", "id", String.valueOf(stepupB))).isZero();
        assertThat(contarPorId("tb_govbr_link_state", "state_id", govbrLinkA.toString())).isEqualTo(1L);
        assertThat(contarPorId("tb_govbr_link_state", "state_id", govbrLinkB.toString())).isZero();

        // 3) Ator A (nao-admin): tabelas sem dono individual continuam negadas.
        assertThat(contarPorId("tb_marketplace_access_token", "id", String.valueOf(tokenId)))
                .as("ator comum nao-admin nao pode ler token de app parceiro")
                .isZero();

        // 4) Administrador: ve tudo.
        setActorAdmin();
        assertThat(contarPorId("tb_marketplace_access_token", "id", String.valueOf(tokenId))).isEqualTo(1L);
        assertThat(contarPorId("operational_function_credentials", "id", String.valueOf(credB))).isEqualTo(1L);

        exec("RESET ROLE");
    }

    private void setActor(long usuarioId) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', '|ROLE_ADVOGADO|', true)")
                .getSingleResult();
    }

    private void setActorAdmin() {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', '|ROLE_ADMINISTRADOR|', true)")
                .getSingleResult();
    }

    private long contarPorId(String tabela, String coluna, String valor) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + tabela + " WHERE " + coluna + "::text = ?1")
                .setParameter(1, valor).getSingleResult()).longValue();
    }

    private Long inserirCredencial(long usuarioId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO operational_function_credentials (usuario_id, function_code, status) "
                                + "VALUES (?1, ?2, 'ATIVA') RETURNING id")
                .setParameter(1, usuarioId).setParameter(2, "FUNC-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirPasskey(long usuarioId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO passkey_sessions (usuario_id, token_hash, expires_at) "
                                + "VALUES (?1, ?2, NOW() + INTERVAL '1 day') RETURNING id")
                .setParameter(1, usuarioId).setParameter(2, "tok-" + usuarioId + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirDecisionStepup(long usuarioId, long processoId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_decision_stepup_consumption (token_jti, act_type, request_hash, "
                                + "processo_id, usuario_id) VALUES (?1, 'ASSINATURA', ?2, ?3, ?4) RETURNING id")
                .setParameter(1, "jti-" + usuarioId + "-" + sufixo)
                .setParameter(2, "hash-" + usuarioId + "-" + sufixo)
                .setParameter(3, processoId)
                .setParameter(4, usuarioId)
                .getSingleResult();
        return id.longValue();
    }

    private UUID inserirGovBrLink(long usuarioId, String sufixo) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_govbr_link_state (state_id, usuario_id, cpf, code_verifier, nonce, "
                                + "expires_at, created_at) VALUES (?1, ?2, '12345678901', ?3, ?4, "
                                + "NOW() + INTERVAL '10 minutes', NOW())")
                .setParameter(1, id).setParameter(2, usuarioId)
                .setParameter(3, "verifier-" + usuarioId + "-" + sufixo)
                .setParameter(4, "nonce-" + usuarioId + "-" + sufixo)
                .executeUpdate();
        return id;
    }

    private Long inserirMarketplaceClientApp(String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_marketplace_client_app (client_id, client_secret_hash, display_name, "
                                + "allowed_scopes, allowed_grants, status) "
                                + "VALUES (?1, ?2, ?3, 'read', 'client_credentials', 'ATIVO') RETURNING id")
                .setParameter(1, "client-" + sufixo)
                .setParameter(2, "secret-hash-" + sufixo)
                .setParameter(3, "App Teste " + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirMarketplaceToken(long clientAppId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_marketplace_access_token (jti, client_app_id, scope, token_hash, status, "
                                + "issued_at, expires_at) VALUES (?1, ?2, 'read', ?3, 'ATIVO', NOW(), "
                                + "NOW() + INTERVAL '1 hour') RETURNING id")
                .setParameter(1, "jti-token-" + sufixo)
                .setParameter(2, clientAppId)
                .setParameter(3, "hash-token-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private void exec(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private Processo.ProcessoBuilder baseProcesso(String numero) {
        return Processo.builder()
                .numeroProcesso(numero)
                .numeroUnificado(numero)
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVEL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO);
    }

    private Usuario novoUsuario(String rotulo, String sufixo, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome("Dono Cred RLS " + rotulo + " " + sufixo);
        usuario.setEmail("dono.cred.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
        usuario.setSenha("x");
        usuario.setCpf(cpf);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }
}
