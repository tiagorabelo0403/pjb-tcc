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
 * Prova que a V351 filtra de verdade: as duas tabelas de papel-institucional (espelhando
 * @PreAuthorize dos controllers) e adv_clientes (ownership por advogado/equipe, reaproveitando as
 * mesmas GUCs de tb_processo), sob role NOSUPERUSER NOBYPASSRLS.
 */
class PjbInqueritoDefensoriaClientesRlsIT extends PjbIntegrationTestBase {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void papelInstitucionalEOwnershipDeClienteFiltramDeVerdade() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_inq_def_cli_probe_" + sufixo;

        Usuario advogadoA = usuarioRepository.save(novoUsuario("A", sufixo, "66677788801"));
        Usuario advogadoB = usuarioRepository.save(novoUsuario("B", sufixo, "66677788802"));

        Long inquerito = inserirInquerito(sufixo);
        Long defensoria = inserirDefensoria(advogadoA.getId(), sufixo);
        Long clienteA = inserirCliente(advogadoA.getId(), sufixo, "A");
        Long clienteB = inserirCliente(advogadoB.getId(), sufixo, "B");
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_inquerito_policial_digital, tb_defensoria_vulnerabilidade_caso, "
                + "adv_clientes TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);

        exec("SET LOCAL ROLE " + role);

        // 1) Sem ator: permissivo (jobs/boot).
        assertThat(contarPorId("tb_inquerito_policial_digital", String.valueOf(inquerito))).isEqualTo(1L);
        assertThat(contarPorId("tb_defensoria_vulnerabilidade_caso", String.valueOf(defensoria))).isEqualTo(1L);

        // 2) Ator sem papel institucional relevante, sem filtro de equipe ativo: ve inquerito/defensoria
        //    negados, mas cliente permissivo (equipe_filter_active nunca foi setado).
        setActorRole(advogadoA.getId(), "ROLE_ADVOGADO");
        assertThat(contarPorId("tb_inquerito_policial_digital", String.valueOf(inquerito)))
                .as("advogado comum nao enxerga inquerito policial")
                .isZero();
        assertThat(contarPorId("tb_defensoria_vulnerabilidade_caso", String.valueOf(defensoria)))
                .as("advogado comum nao enxerga caso de vulnerabilidade da defensoria")
                .isZero();

        // 3) Papel policial: ve o inquerito, nao a defensoria.
        setActorRole(advogadoA.getId(), "ROLE_DELEGADO_POLICIA");
        assertThat(contarPorId("tb_inquerito_policial_digital", String.valueOf(inquerito))).isEqualTo(1L);
        assertThat(contarPorId("tb_defensoria_vulnerabilidade_caso", String.valueOf(defensoria))).isZero();

        // 4) Papel defensor: ve a defensoria, nao o inquerito.
        setActorRole(advogadoA.getId(), "ROLE_DEFENSOR_PUBLICO");
        assertThat(contarPorId("tb_defensoria_vulnerabilidade_caso", String.valueOf(defensoria))).isEqualTo(1L);
        assertThat(contarPorId("tb_inquerito_policial_digital", String.valueOf(inquerito))).isZero();

        // 5) adv_clientes: com filtro de equipe ativo, dono A ve o proprio cliente, nao o de B.
        setEquipeFiltro(advogadoA.getId());
        assertThat(contarPorId("adv_clientes", String.valueOf(clienteA))).isEqualTo(1L);
        assertThat(contarPorId("adv_clientes", String.valueOf(clienteB)))
                .as("banco nega o cliente de outro advogado mesmo com WHERE explicito")
                .isZero();

        exec("RESET ROLE");
    }

    private void setActorRole(long usuarioId, String role) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', ?1, true)")
                .setParameter(1, "|" + role + "|").getSingleResult();
        // desliga o filtro de equipe explicitamente pra nao vazar estado do cenario 5 pros 2-4.
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_filter_active', 'false', true)")
                .getSingleResult();
    }

    private void setEquipeFiltro(long usuarioId) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_filter_active', 'true', true)").getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_usuario_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_id', '-1', true)").getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', '|ROLE_ADVOGADO|', true)").getSingleResult();
    }

    private long contarPorId(String tabela, String id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + tabela + " WHERE id::text = ?1")
                .setParameter(1, id).getSingleResult()).longValue();
    }

    private Long inserirInquerito(String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_inquerito_policial_digital (numero_procedimento, tipo, status, "
                                + "fase_atual, natureza_fato, resumo_fatos) "
                                + "VALUES (?1, 'IP', 'INSTAURADO', 'INVESTIGACAO', 'FURTO', 'resumo teste') "
                                + "RETURNING id")
                .setParameter(1, "IP-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirDefensoria(long defensorId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_defensoria_vulnerabilidade_caso (defensor_id, assistido_nome, "
                                + "score_vulnerabilidade, prioridade_faixa, status) "
                                + "VALUES (?1, ?2, 80, 'ALTA', 'ABERTO') RETURNING id")
                .setParameter(1, defensorId)
                .setParameter(2, "Assistido " + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirCliente(long advogadoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO adv_clientes (advogado_id, nome, nome_normalizado, data_criacao, "
                                + "data_atualizacao) VALUES (?1, ?2, ?3, NOW(), NOW()) RETURNING id")
                .setParameter(1, advogadoId)
                .setParameter(2, "Cliente " + rotulo + " " + sufixo)
                .setParameter(3, ("Cliente " + rotulo + " " + sufixo).toUpperCase())
                .getSingleResult();
        return id.longValue();
    }

    private void exec(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private Usuario novoUsuario(String rotulo, String sufixo, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome("Advogado RLS " + rotulo + " " + sufixo);
        usuario.setEmail("advogado.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
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
