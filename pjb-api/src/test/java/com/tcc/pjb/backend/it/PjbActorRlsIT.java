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
 * Prova que as policies de ator da V343 (support_ticket, legal_ai_audit_log) filtram de verdade no
 * banco, e não são apenas WHERE disfarçado. Como a conexão da aplicação nos Testcontainers é
 * SUPERUSER (que sempre ignora RLS, FORCE ou não), a negação real é observada sob SET LOCAL ROLE para
 * uma role NOSUPERUSER NOBYPASSRLS — simulando a role de runtime (pjb_app) que produção deve usar.
 *
 * <p>Cobre os três mecanismos comuns às quatro policies: contexto de sistema permissivo (GUC vazia),
 * casamento por dono ({@code pjb_rls_actor_id()} — numérico no support_ticket, string no audit) e por
 * papel ({@code pjb_rls_has_role()} — suporte técnico e administrador). judge_travel_exception e
 * intimacao_audiencia reusam exatamente os mesmos helpers e formatos.</p>
 */
class PjbActorRlsIT extends PjbIntegrationTestBase {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void policiesDeAtorFiltramPorDonoEPorPapelSobRoleSemBypass() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_actor_probe_" + sufixo;

        Usuario donoA = usuarioRepository.save(novoUsuario("A", sufixo, "11122233301"));
        Usuario donoB = usuarioRepository.save(novoUsuario("B", sufixo, "11122233302"));
        long idA = donoA.getId();
        long idB = donoB.getId();
        long idEstranho = 999_000_000L + (Math.abs(sufixo.hashCode()) % 1000);

        inserirTicket(idA, "Ticket A " + sufixo);
        inserirTicket(idB, "Ticket B " + sufixo);
        inserirAudit(String.valueOf(idA), "ACAO_A_" + sufixo);
        inserirAudit(String.valueOf(idB), "ACAO_B_" + sufixo);
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON support_ticket TO " + role);
        exec("GRANT SELECT ON legal_ai_audit_log TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);

        exec("SET LOCAL ROLE " + role);

        // 1) Contexto de sistema (GUCs vazias) => permissivo: jobs/boot/migrations não quebram.
        assertThat(contarTickets(idA, idB))
                .as("sem ator definido, a policy é permissiva")
                .isEqualTo(2L);
        assertThat(contarAudit(sufixo)).isEqualTo(2L);

        // 2) Dono A: vê só o próprio ticket e o próprio audit; a linha de B é negada pelo banco.
        setActor(String.valueOf(idA), "");
        assertThat(contarTicketDe(idB))
                .as("banco nega o ticket do outro dono mesmo com WHERE explícito")
                .isZero();
        assertThat(contarTicketDe(idA)).isEqualTo(1L);
        assertThat(contarAuditDe(String.valueOf(idB))).isZero();
        assertThat(contarAuditDe(String.valueOf(idA))).isEqualTo(1L);

        // 3) Papel suporte técnico (ator não-dono): vê TODA a fila de tickets — mas NÃO o audit
        //    (audit exige admin, não suporte).
        setActor(String.valueOf(idEstranho), "|ROLE_SUPORTE_TECNICO|");
        assertThat(contarTickets(idA, idB))
                .as("suporte técnico enxerga a fila inteira de tickets")
                .isEqualTo(2L);
        assertThat(contarAudit(sufixo))
                .as("suporte técnico NÃO é admin: não enxerga audit de terceiros")
                .isZero();

        // 4) Administrador: vê tudo nas duas tabelas.
        setActor(String.valueOf(idEstranho), "|ROLE_ADMINISTRADOR|");
        assertThat(contarTickets(idA, idB)).isEqualTo(2L);
        assertThat(contarAudit(sufixo)).isEqualTo(2L);

        exec("RESET ROLE");
    }

    private void setActor(String actorId, String roles) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, actorId).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', ?1, true)")
                .setParameter(1, roles).getSingleResult();
    }

    private long contarTickets(long idA, long idB) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM support_ticket WHERE aberto_por_id IN (?1, ?2)")
                .setParameter(1, idA).setParameter(2, idB).getSingleResult()).longValue();
    }

    private long contarTicketDe(long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM support_ticket WHERE aberto_por_id = ?1")
                .setParameter(1, id).getSingleResult()).longValue();
    }

    private long contarAudit(String sufixo) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM legal_ai_audit_log WHERE acao LIKE ?1")
                .setParameter(1, "ACAO_%_" + sufixo).getSingleResult()).longValue();
    }

    private long contarAuditDe(String atorId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM legal_ai_audit_log WHERE ator_id = ?1")
                .setParameter(1, atorId).getSingleResult()).longValue();
    }

    private void inserirTicket(long abertoPorId, String assunto) {
        entityManager.createNativeQuery(
                        "INSERT INTO support_ticket (aberto_por_id, aberto_por_nome, categoria, assunto, descricao, status) "
                                + "VALUES (?1, ?2, 'GERAL', ?3, 'descricao', 'ABERTO')")
                .setParameter(1, abertoPorId)
                .setParameter(2, "Dono " + abertoPorId)
                .setParameter(3, assunto)
                .executeUpdate();
    }

    private void inserirAudit(String atorId, String acao) {
        entityManager.createNativeQuery(
                        "INSERT INTO legal_ai_audit_log (id, acao, ator_id) VALUES (gen_random_uuid(), ?1, ?2)")
                .setParameter(1, acao)
                .setParameter(2, atorId)
                .executeUpdate();
    }

    private void exec(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private Usuario novoUsuario(String rotulo, String sufixo, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome("Ator RLS " + rotulo + " " + sufixo);
        usuario.setEmail("ator.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
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
