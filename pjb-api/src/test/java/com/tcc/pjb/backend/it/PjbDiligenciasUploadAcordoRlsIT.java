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
 * Prova que a V354 (6 diligencias + upload + documento_pagina + acordo_mensagem) propaga o
 * ownership de tb_processo corretamente, sob role NOSUPERUSER NOBYPASSRLS.
 */
class PjbDiligenciasUploadAcordoRlsIT extends PjbIntegrationTestBase {

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void diligenciasUploadEAcordoMensagemHerdamOwnershipDeProcesso() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_dilig_probe_" + sufixo;

        Usuario donoA = usuarioRepository.save(novoUsuario("A", sufixo, "99900011122"));
        Usuario donoB = usuarioRepository.save(novoUsuario("B", sufixo, "99900011123"));
        Long processoA = processoRepository.save(baseProcesso("PROC-DIL-A-" + sufixo).usuario(donoA).build()).getId();
        Long processoB = processoRepository.save(baseProcesso("PROC-DIL-B-" + sufixo).usuario(donoB).build()).getId();

        Long encA = inserirEncerramento(processoA, sufixo, "A");
        Long encB = inserirEncerramento(processoB, sufixo, "B");
        Long certDocA = inserirCertidaoDocumento(processoA, sufixo, "A");
        Long certDocB = inserirCertidaoDocumento(processoB, sufixo, "B");
        Long formA = inserirFormalizacao(processoA, sufixo, "A");
        Long formB = inserirFormalizacao(processoB, sufixo, "B");
        Long juntA = inserirJuntada(processoA, sufixo, "A");
        Long juntB = inserirJuntada(processoB, sufixo, "B");
        Long anexA = inserirAnexacao(processoA, sufixo, "A");
        Long anexB = inserirAnexacao(processoB, sufixo, "B");
        Long dispA = inserirMalhaDispatch(processoA, sufixo, "A");
        Long dispB = inserirMalhaDispatch(processoB, sufixo, "B");
        UUID batchA = inserirUploadBatch(processoA, sufixo);
        UUID batchB = inserirUploadBatch(processoB, sufixo);
        UUID itemA = inserirUploadItem(batchA, sufixo, "A");
        UUID itemB = inserirUploadItem(batchB, sufixo, "B");
        UUID documentoA = inserirDocumento(processoA, sufixo, "A");
        UUID documentoB = inserirDocumento(processoB, sufixo, "B");
        UUID paginaA = inserirDocumentoPagina(documentoA, sufixo);
        UUID paginaB = inserirDocumentoPagina(documentoB, sufixo);
        Long sessaoA = inserirSessaoAcordo(processoA, donoA.getId(), sufixo);
        Long sessaoB = inserirSessaoAcordo(processoB, donoB.getId(), sufixo);
        Long msgA = inserirAcordoMensagem(sessaoA, donoA.getId(), sufixo);
        Long msgB = inserirAcordoMensagem(sessaoB, donoB.getId(), sufixo);
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_processo, tb_diligencia_operador_encerramento, "
                + "tb_diligencia_operador_certidao_documento, tb_diligencia_operador_formalizacao_processual, "
                + "tb_diligencia_operador_juntada_processual, tb_diligencia_operador_anexacao_institucional, "
                + "tb_diligencia_operador_malha_institucional_dispatch, tb_upload_batch, tb_upload_item, "
                + "tb_documento_processual, tb_documento_pagina, tb_sessao_acordo_processual, tb_acordo_mensagem "
                + "TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_processo_visivel(bigint) TO " + role);

        exec("SET LOCAL ROLE " + role);
        setEquipeFiltro(donoA.getId());

        assertThat(contarPorId("tb_diligencia_operador_encerramento", "id", String.valueOf(encA))).isEqualTo(1L);
        assertThat(contarPorId("tb_diligencia_operador_encerramento", "id", String.valueOf(encB))).isZero();

        assertThat(contarPorId("tb_diligencia_operador_certidao_documento", "id", String.valueOf(certDocA))).isEqualTo(1L);
        assertThat(contarPorId("tb_diligencia_operador_certidao_documento", "id", String.valueOf(certDocB))).isZero();

        assertThat(contarPorId("tb_diligencia_operador_formalizacao_processual", "id", String.valueOf(formA))).isEqualTo(1L);
        assertThat(contarPorId("tb_diligencia_operador_formalizacao_processual", "id", String.valueOf(formB))).isZero();

        assertThat(contarPorId("tb_diligencia_operador_juntada_processual", "id", String.valueOf(juntA))).isEqualTo(1L);
        assertThat(contarPorId("tb_diligencia_operador_juntada_processual", "id", String.valueOf(juntB))).isZero();

        assertThat(contarPorId("tb_diligencia_operador_anexacao_institucional", "id", String.valueOf(anexA))).isEqualTo(1L);
        assertThat(contarPorId("tb_diligencia_operador_anexacao_institucional", "id", String.valueOf(anexB))).isZero();

        assertThat(contarPorId("tb_diligencia_operador_malha_institucional_dispatch", "id", String.valueOf(dispA))).isEqualTo(1L);
        assertThat(contarPorId("tb_diligencia_operador_malha_institucional_dispatch", "id", String.valueOf(dispB))).isZero();

        assertThat(contarPorId("tb_upload_batch", "id", batchA.toString())).isEqualTo(1L);
        assertThat(contarPorId("tb_upload_batch", "id", batchB.toString())).isZero();
        assertThat(contarPorId("tb_upload_item", "id", itemA.toString())).isEqualTo(1L);
        assertThat(contarPorId("tb_upload_item", "id", itemB.toString())).isZero();

        assertThat(contarPorId("tb_documento_pagina", "id", paginaA.toString())).isEqualTo(1L);
        assertThat(contarPorId("tb_documento_pagina", "id", paginaB.toString())).isZero();

        assertThat(contarPorId("tb_acordo_mensagem", "id", String.valueOf(msgA))).isEqualTo(1L);
        assertThat(contarPorId("tb_acordo_mensagem", "id", String.valueOf(msgB)))
                .as("mensagem de sessao de acordo de processo de outro dono deve ser negada")
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

    private Long inserirEncerramento(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_diligencia_operador_encerramento (operator_user_id, operator_tipo_usuario, "
                                + "canal, diligence_reference, outcome, processo_id, idempotency_key, "
                                + "execution_digest_sha256) VALUES (1, 'OFICIAL_JUSTICA', 'PORTAL', ?1, 'CONCLUIDO', "
                                + "?2, ?3, 'digest') RETURNING id")
                .setParameter(1, "ref-enc-" + rotulo + "-" + sufixo)
                .setParameter(2, processoId)
                .setParameter(3, "idemp-enc-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirCertidaoDocumento(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_diligencia_operador_certidao_documento (certidao_id, processo_id, "
                                + "documento_id, origem) VALUES (1, ?1, ?2, 'SISTEMA') RETURNING id")
                .setParameter(1, processoId)
                .setParameter(2, UUID.randomUUID())
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirFormalizacao(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_diligencia_operador_formalizacao_processual (operator_user_id, "
                                + "operator_tipo_usuario, canal, diligence_reference, processo_id, certidao_id, "
                                + "idempotency_key, formalization_digest_sha256) VALUES (1, 'OFICIAL_JUSTICA', "
                                + "'PORTAL', ?1, ?2, 1, ?3, 'digest') RETURNING id")
                .setParameter(1, "ref-form-" + rotulo + "-" + sufixo)
                .setParameter(2, processoId)
                .setParameter(3, "idemp-form-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirJuntada(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_diligencia_operador_juntada_processual (operator_user_id, "
                                + "operator_tipo_usuario, canal, diligence_reference, processo_id, formalizacao_id, "
                                + "bundle_digest_sha256, bundle_signature_hmac_sha256, idempotency_key) "
                                + "VALUES (1, 'OFICIAL_JUSTICA', 'PORTAL', ?1, ?2, 1, 'digest', 'hmac', ?3) "
                                + "RETURNING id")
                .setParameter(1, "ref-junt-" + rotulo + "-" + sufixo)
                .setParameter(2, processoId)
                .setParameter(3, "idemp-junt-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirAnexacao(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_diligencia_operador_anexacao_institucional (operator_user_id, "
                                + "operator_tipo_usuario, canal, diligence_reference, processo_id, juntada_id, "
                                + "bundle_digest_sha256, bundle_signature_hmac_sha256, external_system_code, "
                                + "destination_box, ack_protocol, ack_reference, annexation_status, "
                                + "external_receipt_digest_sha256, chain_idempotency_key, request_hash_sha256, "
                                + "execution_digest_sha256) VALUES (1, 'OFICIAL_JUSTICA', 'PORTAL', ?1, ?2, 1, "
                                + "'digest', 'hmac', 'SISTEMA', 'caixa', 'protocolo', 'ack-ref', 'CONCLUIDO', "
                                + "'digest-receipt', ?3, 'req-hash', 'exec-digest') RETURNING id")
                .setParameter(1, "ref-anex-" + rotulo + "-" + sufixo)
                .setParameter(2, processoId)
                .setParameter(3, "idemp-anex-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private Long inserirMalhaDispatch(long processoId, String sufixo, String rotulo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_diligencia_operador_malha_institucional_dispatch (operator_user_id, "
                                + "operator_tipo_usuario, canal, diligence_reference, processo_id, annexation_id, "
                                + "outbox_event_id, event_type, routing_key, external_system_code, destination_box, "
                                + "mesh_org_key, mesh_unit_key, dispatch_status, replay_token, "
                                + "chain_idempotency_key, request_hash_sha256, payload_digest_sha256, "
                                + "payload_signature_hmac_sha256, created_at) "
                                + "VALUES (1, 'OFICIAL_JUSTICA', 'PORTAL', ?1, ?2, 1, ?3, 'EVT', 'rk', 'SISTEMA', "
                                + "'caixa', 'org', 'unit', 'ENVIADO', ?4, ?5, 'req-hash', 'payload-digest', "
                                + "'payload-hmac', NOW()) RETURNING id")
                .setParameter(1, "ref-disp-" + rotulo + "-" + sufixo)
                .setParameter(2, processoId)
                .setParameter(3, UUID.randomUUID())
                .setParameter(4, "replay-" + rotulo + "-" + sufixo)
                .setParameter(5, "idemp-disp-" + rotulo + "-" + sufixo)
                .getSingleResult();
        return id.longValue();
    }

    private UUID inserirUploadBatch(long processoId, String sufixo) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_upload_batch (id, processo_id, status, created_at) "
                                + "VALUES (?1, ?2, 'ABERTO', NOW())")
                .setParameter(1, id).setParameter(2, processoId)
                .executeUpdate();
        return id;
    }

    private UUID inserirUploadItem(UUID batchId, String sufixo, String rotulo) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_upload_item (id, batch_id, status) VALUES (?1, ?2, 'PENDENTE')")
                .setParameter(1, id).setParameter(2, batchId)
                .executeUpdate();
        return id;
    }

    private UUID inserirDocumento(long processoId, String sufixo, String rotulo) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_documento_processual (id, processo_id, titulo, categoria) "
                                + "VALUES (?1, ?2, ?3, 'PUBLICO')")
                .setParameter(1, id).setParameter(2, processoId)
                .setParameter(3, "Documento " + rotulo + " " + sufixo)
                .executeUpdate();
        return id;
    }

    private UUID inserirDocumentoPagina(UUID documentoId, String sufixo) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_documento_pagina (id, documento_id, page_number, page_id, fingerprint) "
                                + "VALUES (?1, ?2, 1, ?3, ?4)")
                .setParameter(1, id).setParameter(2, documentoId)
                .setParameter(3, "page-" + sufixo + "-" + id)
                .setParameter(4, "fp-" + sufixo + "-" + id)
                .executeUpdate();
        return id;
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

    private Long inserirAcordoMensagem(long sessaoId, long autorId, String sufixo) {
        Number id = (Number) entityManager.createNativeQuery(
                        "INSERT INTO tb_acordo_mensagem (sessao_id, autor_id, tipo, conteudo, visibilidade) "
                                + "VALUES (?1, ?2, 'TEXTO', 'conteudo teste " + sufixo + "', 'PARTICIPANTES') "
                                + "RETURNING id")
                .setParameter(1, sessaoId).setParameter(2, autorId)
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
        usuario.setNome("Dono Diligencia RLS " + rotulo + " " + sufixo);
        usuario.setEmail("dono.diligencia.rls." + rotulo.toLowerCase() + "." + sufixo + "@test.local");
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
