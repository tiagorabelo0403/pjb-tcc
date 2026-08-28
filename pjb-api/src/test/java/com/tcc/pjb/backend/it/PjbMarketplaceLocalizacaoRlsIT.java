package com.tcc.pjb.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prova que a V352 filtra de verdade: tb_marketplace_client_app/webhook_endpoint (system+admin
 * only) e tb_pessoa_localizacao_consulta (ownership por executor_user_id), sob role
 * NOSUPERUSER NOBYPASSRLS.
 */
class PjbMarketplaceLocalizacaoRlsIT extends PjbIntegrationTestBase {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void marketplaceSystemAdminOnly_localizacaoPorExecutor() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_mkt_loc_probe_" + sufixo;

        Usuario executorA = usuarioRepository.save(novoUsuario("A", sufixo, "77788899901"));
        Usuario executorB = usuarioRepository.save(novoUsuario("B", sufixo, "77788899902"));

        Long clientAppId = inserirClientApp(sufixo);
        Long webhookId = inserirWebhook(clientAppId, sufixo);
        Long consultaA = inserirConsulta(executorA.getId(), sufixo, "A");
        Long consultaB = inserirConsulta(executorB.getId(), sufixo, "B");
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_marketplace_client_app, tb_marketplace_webhook_endpoint, "
                + "tb_pessoa_localizacao_consulta TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);

        exec("SET LOCAL ROLE " + role);

        // 1) Sem ator: permissivo.
        assertThat(contarPorId("tb_marketplace_client_app", String.valueOf(clientAppId))).isEqualTo(1L);
        assertThat(contarPorId("tb_marketplace_webhook_endpoint", String.valueOf(webhookId))).isEqualTo(1L);

        // 2) Ator A comum: nega marketplace, ve a propria consulta, nega a de B.
        setActor(executorA.getId());
        assertThat(contarPorId("tb_marketplace_client_app", String.valueOf(clientAppId)))
                .as("ator comum nao-admin nao le app parceiro")
                .isZero();
        assertThat(contarPorId("tb_marketplace_webhook_endpoint", String.valueOf(webhookId))).isZero();
        assertThat(contarPorId("tb_pessoa_localizacao_consulta", String.valueOf(consultaA))).isEqualTo(1L);
        assertThat(contarPorId("tb_pessoa_localizacao_consulta", String.valueOf(consultaB)))
                .as("banco nega a consulta de localizacao executada por outro usuario")
                .isZero();

        // 3) Admin: ve tudo.
        setActorAdmin();
        assertThat(contarPorId("tb_marketplace_client_app", String.valueOf(clientAppId))).isEqualTo(1L);
        assertThat(contarPorId("tb_pessoa_localizacao_consulta", String.valueOf(consultaB))).isEqualTo(1L);

        exec("RESET ROLE");
    }

    private void setActor(long usuarioId) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', '|ROLE_ADVOGADO|', true)").getSingleResult();
    }

    private void setActorAdmin() {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', '|ROLE_ADMINISTRADOR|', true)").getSingleResult();
    }

    private long contarPorId(String tabela, String id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + tabela + " WHERE id::text = ?1")
                .setParameter(1, id).getSingleResult()).longValue();
    }

    private Long inserirClientApp(String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_marketplace_client_app (client_id, client_secret_hash, display_name, "
                                + "allowed_scopes, allowed_grants, status) "
                                + "VALUES (?1, ?2, ?3, 'read', 'client_credentials', 'ATIVO') RETURNING id")
                .setParameter(1, "client-loc-" + sufixo)
                .setParameter(2, "secret-hash-" + sufixo)
                .setParameter(3, "App Loc Teste " + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirWebhook(long clientAppId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_marketplace_webhook_endpoint (client_app_id, callback_url, event_filter, "
                                + "signing_secret_hash, status) VALUES (?1, ?2, 'evento.*', ?3, 'ATIVO') RETURNING id")
                .setParameter(1, clientAppId)
                .setParameter(2, "https://example.test/webhook/" + sufixo)
                .setParameter(3, "signing-hash-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirConsulta(long executorUserId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_pessoa_localizacao_consulta (correlation_id, executor_user_id, "
                                + "executor_tipo_usuario, canal_consulta, fundamento, referencia_procedimental, "
                                + "finalidade, justificativa_operacional, cpf_hash, cpf_mascarado, "
                                + "possui_contexto_formal, consulta_sem_processo_autorizada, "
                                + "endereco_estrito_solicitado, endereco_estrito_liberado, nivel_exposicao, "
                                + "postura_nivel, postura_score, requer_revisao, modo_liberacao, sinais_postura) "
                                + "VALUES (?1, ?2, 'ADVOGADO', 'PORTAL', 'MANDADO_JUDICIAL', ?3, 'teste', 'teste', "
                                + "?4, '***', true, false, false, false, 'BAIXA', 'NORMAL', 10, false, "
                                + "'AUTOMATICO', '[]') RETURNING id")
                .setParameter(1, "corr-" + rotulo + "-" + sufixo)
                .setParameter(2, executorUserId)
                .setParameter(3, "ref-" + rotulo + "-" + sufixo)
                .setParameter(4, "hash-cpf-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private void exec(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private Usuario novoUsuario(String rotulo, String sufixo, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome("Executor Loc RLS " + rotulo + " " + sufixo);
        usuario.setEmail("executor.loc.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
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
