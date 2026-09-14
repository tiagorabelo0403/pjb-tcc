package com.tcc.pjb.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prova que as policies da V346 (tb_credencial_acesso, tb_boletim_ocorrencia_digital) filtram de
 * verdade no banco sob uma role NOSUPERUSER NOBYPASSRLS (simulando pjb_app), reusando exatamente os
 * mesmos helpers da V343 ({@code pjb_rls_system_context}/{@code pjb_rls_has_role}).
 */
class PjbCredencialBoletimRlsIT extends PjbIntegrationTestBase {

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    InstituicaoRepository instituicaoRepository;

    @Autowired
    UnidadeInstituicaoRepository unidadeRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void credencialSoVisivelParaSistemaOuAdmin_boletimSoVisivelParaPapelPolicialOuAdmin() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_cred_boletim_probe_" + sufixo;

        Long processoId = processoRepository.save(novoProcesso("RLS-CRED-" + sufixo)).getId();
        inserirCredencial(processoId, "login." + sufixo, sufixo);

        Instituicao instituicao = instituicaoRepository.save(novaInstituicao(sufixo));
        UnidadeInstituicao unidade = unidadeRepository.save(novaUnidade(instituicao, sufixo));
        Usuario delegado = usuarioRepository.save(novoUsuario(sufixo));
        inserirBoletim(unidade.getId(), delegado.getId(), sufixo);
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON tb_credencial_acesso TO " + role);
        exec("GRANT SELECT ON tb_boletim_ocorrencia_digital TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_actor_id() TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_has_role(text) TO " + role);
        exec("GRANT EXECUTE ON FUNCTION pjb_rls_system_context() TO " + role);

        exec("SET LOCAL ROLE " + role);

        // 1) Contexto de sistema (GUCs vazias): permissivo nas duas tabelas.
        assertThat(contarCredencial(sufixo))
                .as("sem ator definido, tb_credencial_acesso e permissiva (boot/migration/anonimo)")
                .isEqualTo(1L);
        assertThat(contarBoletim(sufixo))
                .as("sem ator definido, tb_boletim_ocorrencia_digital e permissiva")
                .isEqualTo(1L);

        // 2) Ator autenticado comum (nem admin, nem policial): banco nega as duas.
        long idEstranho = 999_000_000L + (Math.abs(sufixo.hashCode()) % 1000);
        setActor(String.valueOf(idEstranho), "|ROLE_ADVOGADO|");
        assertThat(contarCredencial(sufixo))
                .as("ator comum nao-admin nao pode ler credencial de acesso")
                .isZero();
        assertThat(contarBoletim(sufixo))
                .as("ator comum sem papel policial nao pode ler boletim de ocorrencia")
                .isZero();

        // 3) Papel policial: ve o boletim, mas NAO a credencial (credencial exige admin/sistema).
        setActor(String.valueOf(idEstranho), "|ROLE_DELEGADO_POLICIA|");
        assertThat(contarBoletim(sufixo))
                .as("delegado de policia enxerga o boletim, espelhando o @PreAuthorize do controller")
                .isEqualTo(1L);
        assertThat(contarCredencial(sufixo))
                .as("delegado de policia nao e admin: nao enxerga credencial de acesso")
                .isZero();

        // 4) Administrador: ve as duas.
        setActor(String.valueOf(idEstranho), "|ROLE_ADMINISTRADOR|");
        assertThat(contarCredencial(sufixo)).isEqualTo(1L);
        assertThat(contarBoletim(sufixo)).isEqualTo(1L);

        exec("RESET ROLE");
    }

    private void setActor(String actorId, String roles) {
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_id', ?1, true)")
                .setParameter(1, actorId).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.pjb_actor_roles', ?1, true)")
                .setParameter(1, roles).getSingleResult();
    }

    private long contarCredencial(String sufixo) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM tb_credencial_acesso WHERE login = ?1")
                .setParameter(1, "login." + sufixo).getSingleResult()).longValue();
    }

    private long contarBoletim(String sufixo) {
        return ((Number) entityManager
                .createNativeQuery("SELECT count(*) FROM tb_boletim_ocorrencia_digital WHERE numero_boletim = ?1")
                .setParameter(1, "BOL-" + sufixo).getSingleResult()).longValue();
    }

    private void inserirCredencial(long processoId, String login, String sufixo) {
        entityManager.createNativeQuery(
                        "INSERT INTO tb_credencial_acesso (login, senha_hash, processo_id) VALUES (?1, ?2, ?3)")
                .setParameter(1, login)
                .setParameter(2, "hash-" + sufixo)
                .setParameter(3, processoId)
                .executeUpdate();
    }

    private void inserirBoletim(long unidadeId, long registradoPorId, String sufixo) {
        entityManager.createNativeQuery(
                        "INSERT INTO tb_boletim_ocorrencia_digital "
                                + "(uuid, numero_boletim, status, natureza_fato, resumo_fatos, local_fato, "
                                + "ocorrido_em, comunicante_resumo, providencias_iniciais, unidade_registro_id, "
                                + "registrado_por_id, cadeia_custodia_hash) "
                                + "VALUES (gen_random_uuid(), ?1, 'REGISTRADO', 'FURTO', 'resumo teste', 'local teste', "
                                + "NOW(), 'comunicante teste', 'providencias teste', ?2, ?3, ?4)")
                .setParameter(1, "BOL-" + sufixo)
                .setParameter(2, unidadeId)
                .setParameter(3, registradoPorId)
                .setParameter(4, "hash-custodia-" + sufixo)
                .executeUpdate();
    }

    private void exec(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private Processo novoProcesso(String numero) {
        return Processo.builder()
                .numeroProcesso(numero)
                .numeroUnificado(numero)
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.PENAL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build();
    }

    private Instituicao novaInstituicao(String sufixo) {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        instituicao.setNome("Delegacia RLS " + sufixo);
        return instituicao;
    }

    private UnidadeInstituicao novaUnidade(Instituicao instituicao, String sufixo) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome("Delegacia RLS " + sufixo);
        unidade.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        unidade.setComarca("Fortaleza");
        unidade.setUf("CE");
        return unidade;
    }

    private Usuario novoUsuario(String sufixo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Delegado RLS " + sufixo);
        usuario.setEmail("delegado.rls." + sufixo + "@test.local");
        usuario.setSenha("x");
        usuario.setCpf("98765432100");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }
}
