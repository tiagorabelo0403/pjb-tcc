package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.secretariat.SecretariaInstitucionalFilaResponse;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reforco 3: prova que a politica RLS de secretaria_institucional_item nao e so uma query WHERE
 * disfarcada de politica de banco. A conexao da aplicacao (usuario "pjb" no Testcontainers) e
 * SUPERUSER — Postgres SEMPRE deixa superuser ignorar RLS, FORCE ou nao. Por isso o teste de negacao
 * real (metodo b) usa SET LOCAL ROLE para uma role sem BYPASSRLS criada so para o teste, que e a
 * unica forma de observar a politica agindo de verdade dentro desta mesma conexao/transacao —
 * simulando como a role de runtime da aplicacao DEVERIA estar configurada em producao.
 */
class SecretariaInstitucionalFilaRlsIT extends PjbIntegrationTestBase {

    @Autowired
    SecretariaInstitucionalFilaService filaService;

    @Autowired
    InstituicaoRepository instituicaoRepository;

    @Autowired
    UnidadeInstituicaoRepository unidadeRepository;

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    SecretariaInstitucionalItemRepository itemRepository;

    @Autowired
    LotacaoInstituicaoRepository lotacaoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void consultarFilaComVisibilidadeSoTrazItensDaPropriaUnidade() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Instituicao instituicao = instituicaoRepository.save(novaInstituicao(sufixo));
        UnidadeInstituicao unidadeX = unidadeRepository.save(novaUnidade(instituicao, "Promotoria RLS X " + sufixo, "Fortaleza"));
        UnidadeInstituicao unidadeY = unidadeRepository.save(novaUnidade(instituicao, "Promotoria RLS Y " + sufixo, "Sobral"));

        Long processoX = processoRepository.save(novoProcesso("RLS-X-" + sufixo)).getId();
        Long processoY = processoRepository.save(novoProcesso("RLS-Y-" + sufixo)).getId();

        itemRepository.save(novoItem(processoX, unidadeX.getId()));
        itemRepository.save(novoItem(processoY, unidadeY.getId()));

        Usuario promotor = usuarioRepository.save(novoPromotor(sufixo));
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(promotor);
        lotacao.setUnidade(unidadeX);
        lotacao.setInicio(LocalDate.now().minusYears(1));
        lotacaoRepository.save(lotacao);

        SecretariaInstitucionalFilaResponse resposta = filaService.consultarFila(promotor, unidadeX.getId());

        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.itens().get(0).processoId()).isEqualTo(processoX);
    }

    @Test
    @Transactional
    void rlsNegaLinhaDeOutraUnidadeMesmoQuandoQueryTentaBuscarDiretamenteAConexaoDaAplicacaoESuperuser() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String role = "pjb_rls_probe_" + sufixo;

        Instituicao instituicao = instituicaoRepository.save(novaInstituicao(sufixo));
        UnidadeInstituicao unidadeX = unidadeRepository.save(novaUnidade(instituicao, "Promotoria RLS Deny X " + sufixo, "Fortaleza"));
        UnidadeInstituicao unidadeY = unidadeRepository.save(novaUnidade(instituicao, "Promotoria RLS Deny Y " + sufixo, "Sobral"));

        Long processoX = processoRepository.save(novoProcesso("RLS-DENY-X-" + sufixo)).getId();
        Long processoY = processoRepository.save(novoProcesso("RLS-DENY-Y-" + sufixo)).getId();

        itemRepository.save(novoItem(processoX, unidadeX.getId()));
        itemRepository.save(novoItem(processoY, unidadeY.getId()));
        entityManager.flush();

        exec("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS NOLOGIN");
        exec("GRANT USAGE ON SCHEMA public TO " + role);
        exec("GRANT SELECT ON secretaria_institucional_item TO " + role);

        exec("SET LOCAL ROLE " + role);

        Number semEscopoDefinido = (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM secretaria_institucional_item WHERE unidade_institucional_id IN (?1, ?2)")
                .setParameter(1, unidadeX.getId())
                .setParameter(2, unidadeY.getId())
                .getSingleResult();
        assertThat(semEscopoDefinido.longValue())
                .as("sem app.pjb_secretaria_unidade_id definido, a politica e permissiva por padrao — nao afeta Tasks 2/3/6/7")
                .isEqualTo(2L);

        entityManager.createNativeQuery("SELECT set_config('app.pjb_secretaria_unidade_id', ?1, true)")
                .setParameter(1, unidadeX.getId().toString())
                .getSingleResult();

        Number tentativaDeBurlarBuscandoUnidadeYDiretoNoWhere = (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM secretaria_institucional_item WHERE unidade_institucional_id = ?1")
                .setParameter(1, unidadeY.getId())
                .getSingleResult();
        assertThat(tentativaDeBurlarBuscandoUnidadeYDiretoNoWhere.longValue())
                .as("o banco nega a linha da unidade Y mesmo que a query Java peca por ela explicitamente no WHERE")
                .isZero();

        Number buscaLegitimaDaUnidadeX = (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM secretaria_institucional_item WHERE unidade_institucional_id = ?1")
                .setParameter(1, unidadeX.getId())
                .getSingleResult();
        assertThat(buscaLegitimaDaUnidadeX.longValue()).isEqualTo(1L);

        exec("RESET ROLE");
    }

    private void exec(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private Instituicao novaInstituicao(String sufixo) {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        instituicao.setNome("MP RLS " + sufixo);
        return instituicao;
    }

    private UnidadeInstituicao novaUnidade(Instituicao instituicao, String nome, String comarca) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome(nome);
        unidade.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        unidade.setComarca(comarca);
        unidade.setUf("CE");
        return unidade;
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

    private SecretariaInstitucionalItem novoItem(Long processoId, Long unidadeId) {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        item.setProcessoId(processoId);
        item.setUnidadeInstitucionalId(unidadeId);
        item.setTipoInstituicaoAlvo(TipoUnidadeInstitucional.PROMOTORIA);
        item.setMotivo(MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA);
        item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
        item.setPrazoBaseDias(15);
        item.setPrazoEmDobro(false);
        item.setCriadoEm(Instant.now());
        return item;
    }

    private Usuario novoPromotor(String sufixo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Promotor RLS Teste " + sufixo);
        usuario.setEmail("promotor.rls." + sufixo + "@test.local");
        usuario.setSenha("x");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        usuario.setPerfil(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }
}
