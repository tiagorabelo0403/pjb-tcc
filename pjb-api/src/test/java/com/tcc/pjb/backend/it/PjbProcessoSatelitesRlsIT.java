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
 * Prova que a V348 propaga corretamente o ownership de tb_processo (V347) para as
 * tabelas-satelite (documento_processual, polo_processual, audiencia, acordao via
 * julgamento_colegiado), sob role NOSUPERUSER NOBYPASSRLS.
 */
class PjbProcessoSatelitesRlsIT extends PjbIntegrationTestBase {

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void satelitesHerdamOOwnershipDeProcessoViaExists() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_processo_sat_probe_" + sufixo;

        Usuario donoA = usuarioRepository.save(novoUsuario("A", sufixo, "33344455501"));
        Usuario donoB = usuarioRepository.save(novoUsuario("B", sufixo, "33344455502"));

        Long processoA = processoRepository.save(baseProcesso("PROC-SAT-A-" + sufixo).usuario(donoA).build()).getId();
        Long processoB = processoRepository.save(baseProcesso("PROC-SAT-B-" + sufixo).usuario(donoB).build()).getId();

        UUID documentoA = inserirDocumento(processoA, sufixo, "A");
        UUID documentoB = inserirDocumento(processoB, sufixo, "B");
        Long poloA = inserirPolo(processoA, sufixo, "A");
        Long poloB = inserirPolo(processoB, sufixo, "B");
        Long audienciaA = inserirAudiencia(processoA, sufixo, "A");
        Long audienciaB = inserirAudiencia(processoB, sufixo, "B");
        Long audienciaSemProcesso = inserirAudienciaSemProcesso(sufixo);
        Long julgamentoA = inserirJulgamento(processoA);
        Long julgamentoB = inserirJulgamento(processoB);
        Long acordaoA = inserirAcordao(julgamentoA, sufixo, "A");
        Long acordaoB = inserirAcordao(julgamentoB, sufixo, "B");
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_processo, tb_documento_processual, tb_polo_processual, "
                + "tb_audiencia, tb_acordao, tb_julgamento_colegiado TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_processo_visivel(bigint) TO " + role);

        exec("SET LOCAL ROLE " + role);
        setEquipeFiltro(donoA.getId());

        assertThat(contarPorId("tb_documento_processual", "id", documentoA.toString())).isEqualTo(1L);
        assertThat(contarPorId("tb_documento_processual", "id", documentoB.toString()))
                .as("documento de processo de outro dono deve ser negado pelo banco")
                .isZero();

        assertThat(contarPorId("tb_polo_processual", "id", String.valueOf(poloA))).isEqualTo(1L);
        assertThat(contarPorId("tb_polo_processual", "id", String.valueOf(poloB))).isZero();

        assertThat(contarPorId("tb_audiencia", "id", String.valueOf(audienciaA))).isEqualTo(1L);
        assertThat(contarPorId("tb_audiencia", "id", String.valueOf(audienciaB))).isZero();
        assertThat(contarPorId("tb_audiencia", "id", String.valueOf(audienciaSemProcesso)))
                .as("audiencia sem processo vinculado e' permissiva (nao ha o que restringir)")
                .isEqualTo(1L);

        assertThat(contarPorId("tb_acordao", "id", String.valueOf(acordaoA))).isEqualTo(1L);
        assertThat(contarPorId("tb_acordao", "id", String.valueOf(acordaoB)))
                .as("acordao do julgamento de outro dono (via julgamento_id -> processo_id) deve ser negado")
                .isZero();

        exec("RESET ROLE");
    }

    private void setEquipeFiltro(long usuarioId) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_filter_active', 'true', true)")
                .getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_usuario_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_equipe_id', '-1', true)")
                .getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, String.valueOf(usuarioId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', '|ROLE_ADVOGADO|', true)")
                .getSingleResult();
    }

    private long contarPorId(String tabela, String coluna, String valor) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM " + tabela + " WHERE " + coluna + "::text = ?1")
                .setParameter(1, valor).getSingleResult()).longValue();
    }

    private UUID inserirDocumento(long processoId, String sufixo, String rotulo) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_documento_processual (id, processo_id, titulo, categoria) VALUES (?1, ?2, ?3, 'PUBLICO')")
                .setParameter(1, id)
                .setParameter(2, processoId)
                .setParameter(3, "Documento " + rotulo + " " + sufixo)
                .executeUpdate();
        return id;
    }

    private Long inserirPolo(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_polo_processual (processo_id, tipo_polo, tipo_parte, nome_completo) "
                                + "VALUES (?1, 'ATIVO', 'PESSOA_FISICA', ?2) RETURNING id")
                .setParameter(1, processoId)
                .setParameter(2, "Parte " + rotulo + " " + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirAudiencia(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_audiencia (processo_id, tipo, modalidade, status, data_hora) "
                                + "VALUES (?1, 'INSTRUCAO', 'VIRTUAL', 'DESIGNADA', NOW()) RETURNING id")
                .setParameter(1, processoId)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirAudienciaSemProcesso(String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_audiencia (processo_id, tipo, modalidade, status, data_hora) "
                                + "VALUES (NULL, 'INSTRUCAO', 'VIRTUAL', 'DESIGNADA', NOW()) RETURNING id")
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirJulgamento(long processoId) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_julgamento_colegiado (processo_id, grau, status) "
                                + "VALUES (?1, 'SEGUNDO_GRAU', 'PAUTADO') RETURNING id")
                .setParameter(1, processoId)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirAcordao(long julgamentoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_acordao (julgamento_id, numero_acordao) VALUES (?1, ?2) RETURNING id")
                .setParameter(1, julgamentoId)
                .setParameter(2, "ACORDAO-" + rotulo + "-" + sufixo)
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
        usuario.setNome("Dono Satelite RLS " + rotulo + " " + sufixo);
        usuario.setEmail("dono.satelite.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
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
