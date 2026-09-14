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
 * Prova que a V353 filtra de verdade: tb_sigilo_access_request (processo_id direto) e
 * tb_voto_colegiado (via julgamento_id -> processo_id), sob role NOSUPERUSER NOBYPASSRLS.
 */
class PjbVotoSigiloAccessRlsIT extends PjbIntegrationTestBase {

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void sigiloAccessEVotoColegiadoHerdamOwnershipDeProcesso() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_voto_sigilo_probe_" + sufixo;

        Usuario donoA = usuarioRepository.save(novoUsuario("A", sufixo, "88899900011"));
        Usuario donoB = usuarioRepository.save(novoUsuario("B", sufixo, "88899900012"));

        Long processoA = processoRepository.save(baseProcesso("PROC-V6-A-" + sufixo).usuario(donoA).build()).getId();
        Long processoB = processoRepository.save(baseProcesso("PROC-V6-B-" + sufixo).usuario(donoB).build()).getId();

        UUID sigiloA = inserirSigiloAccessRequest(processoA, donoA.getId(), sufixo);
        UUID sigiloB = inserirSigiloAccessRequest(processoB, donoB.getId(), sufixo);
        Long julgamentoA = inserirJulgamento(processoA);
        Long julgamentoB = inserirJulgamento(processoB);
        Long votoA = inserirVoto(julgamentoA, sufixo, "A");
        Long votoB = inserirVoto(julgamentoB, sufixo, "B");
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_processo, tb_sigilo_access_request, tb_voto_colegiado, "
                + "tb_julgamento_colegiado TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_processo_visivel(bigint) TO " + role);

        exec("SET LOCAL ROLE " + role);
        setEquipeFiltro(donoA.getId());

        assertThat(contarPorId("tb_sigilo_access_request", "id", sigiloA.toString())).isEqualTo(1L);
        assertThat(contarPorId("tb_sigilo_access_request", "id", sigiloB.toString())).isZero();

        assertThat(contarPorId("tb_voto_colegiado", "id", String.valueOf(votoA))).isEqualTo(1L);
        assertThat(contarPorId("tb_voto_colegiado", "id", String.valueOf(votoB)))
                .as("voto do julgamento de processo de outro dono deve ser negado")
                .isZero();

        exec("RESET ROLE");
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

    private long contarPorId(String tabela, String coluna, String valor) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + tabela + " WHERE " + coluna + "::text = ?1")
                .setParameter(1, valor).getSingleResult()).longValue();
    }

    private UUID inserirSigiloAccessRequest(long processoId, long advogadoId, String sufixo) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_sigilo_access_request (id, processo_id, advogado_id, status) "
                                + "VALUES (?1, ?2, ?3, 'PENDENTE')")
                .setParameter(1, id).setParameter(2, processoId).setParameter(3, advogadoId)
                .executeUpdate();
        return id;
    }

    private Long inserirJulgamento(long processoId) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_julgamento_colegiado (processo_id, grau, status) "
                                + "VALUES (?1, 'SEGUNDO_GRAU', 'PAUTADO') RETURNING id")
                .setParameter(1, processoId)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirVoto(long julgamentoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_voto_colegiado (julgamento_id, ordem, magistrado_nome, voto_tipo, "
                                + "proferido_em) VALUES (?1, 1, ?2, 'ACOMPANHA_RELATOR', NOW()) RETURNING id")
                .setParameter(1, julgamentoId)
                .setParameter(2, "Magistrado " + rotulo + " " + sufixo)
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
        usuario.setNome("Dono Voto RLS " + rotulo + " " + sufixo);
        usuario.setEmail("dono.voto.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
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
