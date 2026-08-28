package com.tcc.pjb.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prova que a policy da V347 (tb_processo) espelha o mesmo ownership do @Filter Hibernate
 * "filtroEquipeProcesso" — dono por usuário, dono por equipe, e permissiva quando o filtro nunca é
 * ativado (papel não-advogado), sob role NOSUPERUSER NOBYPASSRLS.
 */
class PjbProcessoEquipeOwnershipRlsIT extends PjbIntegrationTestBase {

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EquipeRepository equipeRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void ownershipPorUsuarioPorEquipeEPermissivoQuandoFiltroInativo() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_processo_probe_" + sufixo;

        Usuario donoA = usuarioRepository.save(novoUsuario("A", sufixo, "22233344401"));
        Usuario donoB = usuarioRepository.save(novoUsuario("B", sufixo, "22233344402"));
        Equipe equipeX = equipeRepository.save(novaEquipe(sufixo));

        Long processoA = processoRepository.save(baseProcesso("PROC-A-" + sufixo).usuario(donoA).build()).getId();
        Long processoB = processoRepository.save(baseProcesso("PROC-B-" + sufixo).usuario(donoB).build()).getId();
        Long processoEquipe = processoRepository.save(baseProcesso("PROC-EQ-" + sufixo).equipe(equipeX).build()).getId();
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_processo TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);

        exec("SET LOCAL ROLE " + role);

        // 1) Filtro nunca ativado (papel nao-advogado, ou sem autenticacao): permissivo, ve os 3.
        assertThat(contarProcessos(processoA, processoB, processoEquipe))
                .as("sem app.pjb_equipe_filter_active=true, a policy e permissiva (mesmo comportamento "
                        + "do @Filter para papeis nao-advogado)")
                .isEqualTo(3L);

        // 2) Filtro ativo, dono = A, sem equipe (-1): so ve o proprio, o banco nega o de B mesmo com WHERE explicito.
        setEquipeFiltro(true, donoA.getId(), -1L);
        assertThat(contarProcessoPorId(processoB))
                .as("banco nega o processo de outro dono mesmo com WHERE explicito")
                .isZero();
        assertThat(contarProcessoPorId(processoA)).isEqualTo(1L);
        assertThat(contarProcessoPorId(processoEquipe))
                .as("nao e dono nem da equipe: nao ve o processo da equipe")
                .isZero();

        // 3) Filtro ativo, ator = B, equipe = X: ve o proprio (por usuario) e o da equipe (por equipe_id).
        setEquipeFiltro(true, donoB.getId(), equipeX.getId());
        assertThat(contarProcessoPorId(processoB)).isEqualTo(1L);
        assertThat(contarProcessoPorId(processoEquipe))
                .as("membro da equipe X ve o processo atribuido a equipe, mesmo sem ser o dono direto")
                .isEqualTo(1L);
        assertThat(contarProcessoPorId(processoA))
                .as("nem dono nem da equipe de A")
                .isZero();

        // 4) Administrador: ve tudo independente do filtro estar ativo.
        setActorRole("ROLE_ADMINISTRADOR");
        assertThat(contarProcessos(processoA, processoB, processoEquipe)).isEqualTo(3L);

        exec("RESET ROLE");
    }

    private void setEquipeFiltro(boolean active, long usuarioId, long equipeId) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_filter_active', ?1, true)")
                .setParameter(1, Boolean.toString(active)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_usuario_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_id', ?1, true)")
                .setParameter(1, String.valueOf(equipeId)).getSingleResult();
        // pjb_rls_system_context() só é permissivo quando actor_id E actor_roles estão vazios —
        // um advogado autenticado de verdade sempre tem os dois preenchidos (mesma conexão que
        // publica as GUCs de equipe também publica actor_id/actor_roles, via
        // PjbProcessoSigiloRlsDataSource). Sem isto, o teste ficaria permissivo o tempo todo pela
        // cláusula de sistema, mascarando a policy de ownership que queremos provar aqui.
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', ?1, true)")
                .setParameter(1, "|ROLE_ADVOGADO|").getSingleResult();
    }

    private void setActorRole(String role) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', ?1, true)")
                .setParameter(1, "|" + role + "|").getSingleResult();
    }

    private long contarProcessos(Long... ids) {
        long total = 0;
        for (Long id : ids) {
            total += contarProcessoPorId(id);
        }
        return total;
    }

    private long contarProcessoPorId(long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM tb_processo WHERE id = ?1")
                .setParameter(1, id).getSingleResult()).longValue();
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
        usuario.setNome("Dono Processo RLS " + rotulo + " " + sufixo);
        usuario.setEmail("dono.processo.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
        usuario.setSenha("x");
        usuario.setCpf(cpf);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }

    private Equipe novaEquipe(String sufixo) {
        Equipe equipe = new Equipe();
        equipe.setNome("Equipe RLS Processo " + sufixo);
        return equipe;
    }
}
