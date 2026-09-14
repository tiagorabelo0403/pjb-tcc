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
 * Prova que a V349 (lote 2 de satelites de tb_processo: pjb_ciencia_processual,
 * tb_expedicao_judicial, tb_processo_note, tb_sessao_acordo_processual)
 * filtra de verdade via pjb_rls_processo_visivel, sob role NOSUPERUSER NOBYPASSRLS.
 */
class PjbProcessoSatelitesLote2RlsIT extends PjbIntegrationTestBase {

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void satelitesLote2HerdamOOwnershipDeProcesso() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_sat2_probe_" + sufixo;

        Usuario donoA = usuarioRepository.save(novoUsuario("A", sufixo, "44455566601"));
        Usuario donoB = usuarioRepository.save(novoUsuario("B", sufixo, "44455566602"));

        Long processoA = processoRepository.save(baseProcesso("PROC-S2-A-" + sufixo).usuario(donoA).build()).getId();
        Long processoB = processoRepository.save(baseProcesso("PROC-S2-B-" + sufixo).usuario(donoB).build()).getId();

        Long cienciaA = inserirCiencia(processoA, donoA.getId(), sufixo, "A");
        Long cienciaB = inserirCiencia(processoB, donoB.getId(), sufixo, "B");
        Long expedicaoA = inserirExpedicao(processoA, sufixo, "A");
        Long expedicaoB = inserirExpedicao(processoB, sufixo, "B");
        Long noteA = inserirNote(processoA, donoA.getId(), sufixo, "A");
        Long noteB = inserirNote(processoB, donoB.getId(), sufixo, "B");
        Long sapA = inserirSessaoAcordo(processoA, donoA.getId(), sufixo);
        Long sapB = inserirSessaoAcordo(processoB, donoB.getId(), sufixo);
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_processo, pjb_ciencia_processual, "
                + "tb_expedicao_judicial, tb_processo_note, tb_sessao_acordo_processual TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_processo_visivel(bigint) TO " + role);

        exec("SET LOCAL ROLE " + role);
        setEquipeFiltro(donoA.getId());

        assertThat(contarPorId("pjb_ciencia_processual", "id", String.valueOf(cienciaA))).isEqualTo(1L);
        assertThat(contarPorId("pjb_ciencia_processual", "id", String.valueOf(cienciaB))).isZero();

        assertThat(contarPorId("tb_expedicao_judicial", "id", String.valueOf(expedicaoA))).isEqualTo(1L);
        assertThat(contarPorId("tb_expedicao_judicial", "id", String.valueOf(expedicaoB))).isZero();

        assertThat(contarPorId("tb_processo_note", "id", String.valueOf(noteA))).isEqualTo(1L);
        assertThat(contarPorId("tb_processo_note", "id", String.valueOf(noteB))).isZero();

        assertThat(contarPorId("tb_sessao_acordo_processual", "id", String.valueOf(sapA))).isEqualTo(1L);
        assertThat(contarPorId("tb_sessao_acordo_processual", "id", String.valueOf(sapB))).isZero();

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

    private Long inserirCiencia(long processoId, long usuarioId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO pjb_ciencia_processual (processo_id, usuario_id, tipo_ciencia, canal, "
                                + "grau_jurisdicao, numero_processo, hash_conteudo, data_disponibilizacao) "
                                + "VALUES (?1, ?2, 'INTIMACAO', 'PORTAL', 'PRIMEIRO_GRAU', ?3, ?4, NOW()) RETURNING id")
                .setParameter(1, processoId).setParameter(2, usuarioId)
                .setParameter(3, "NUM-" + rotulo + "-" + sufixo)
                .setParameter(4, "hash-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirExpedicao(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_expedicao_judicial (expedicao_uuid, processo_id, tipo_comunicacao, "
                                + "modalidade, status, tipo_destinatario, destinatario_nome, destinatario_documento, "
                                + "expedida_em, hash_integridade) VALUES (?1, ?2, 'INTIMACAO', 'DIGITAL', 'EXPEDIDA', "
                                + "'PARTE', ?3, '00000000000', NOW(), ?4) RETURNING id")
                .setParameter(1, UUID.randomUUID().toString())
                .setParameter(2, processoId)
                .setParameter(3, "Destinatario " + rotulo + " " + sufixo)
                .setParameter(4, "hash-exp-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirNote(long processoId, long usuarioId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_processo_note (processo_id, author_usuario_id, author_tipo, body, "
                                + "created_at, updated_at) VALUES (?1, ?2, 'ADVOGADO', ?3, NOW(), NOW()) RETURNING id")
                .setParameter(1, processoId).setParameter(2, usuarioId)
                .setParameter(3, "Nota " + rotulo + " " + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirSessaoAcordo(long processoId, long abertaPorId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_sessao_acordo_processual (processo_id, tipo_sala, status, aberta_por_id, "
                                + "aberta_em, expira_em, motivo_abertura, confidencialidade_nivel) "
                                + "VALUES (?1, 'CONCILIACAO', 'OPEN', ?2, NOW(), NOW() + INTERVAL '1 day', "
                                + "'motivo teste " + sufixo + "', 'PUBLICA_CONTROLADA') RETURNING id")
                .setParameter(1, processoId).setParameter(2, abertaPorId)
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
        usuario.setNome("Dono Satelite2 RLS " + rotulo + " " + sufixo);
        usuario.setEmail("dono.satelite2.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
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
