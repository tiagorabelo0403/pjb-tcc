package com.tcc.pjb.backend.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prova que o Hibernate {@code @Filter} "filtroUnidadeInstitucional" (Reforco 2, camada ORM de
 * defesa em profundidade sobre SecretariaInstitucionalFilaService) realmente restringe linhas —
 * usando itemRepository.findAll(), uma query que NAO tem WHERE por unidade, ao contrario da query
 * de producao (findByUnidadeInstitucionalIdOrderByPrazoFatalAsc). Se o filtro fosse decorativo, o
 * findAll() traria as duas unidades.
 */
class SecretariaInstitucionalItemHibernateFilterIT extends PjbIntegrationTestBase {

    @Autowired
    InstituicaoRepository instituicaoRepository;

    @Autowired
    UnidadeInstituicaoRepository unidadeRepository;

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    SecretariaInstitucionalItemRepository itemRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    void filtroHibernateRestringeMesmoQuandoQueryNaoFiltraPorUnidadeExplicitamente() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        instituicao.setNome("MP Filtro Hibernate " + sufixo);
        instituicao = instituicaoRepository.save(instituicao);

        UnidadeInstituicao unidadeX = novaUnidade(instituicao, "Promotoria X " + sufixo, "Fortaleza");
        UnidadeInstituicao unidadeY = novaUnidade(instituicao, "Promotoria Y " + sufixo, "Sobral");

        Long processoX = novoProcesso("FILTRO-HIB-X-" + sufixo).getId();
        Long processoY = novoProcesso("FILTRO-HIB-Y-" + sufixo).getId();

        SecretariaInstitucionalItem itemX = itemRepository.save(novoItem(processoX, unidadeX.getId()));
        SecretariaInstitucionalItem itemY = itemRepository.save(novoItem(processoY, unidadeY.getId()));

        List<SecretariaInstitucionalItem> antesDoFiltro = itemRepository.findAll();
        assertThat(antesDoFiltro).extracting(SecretariaInstitucionalItem::getId)
                .contains(itemX.getId(), itemY.getId());

        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("filtroUnidadeInstitucional")
                .setParameter("unidadeInstitucionalIdParam", unidadeX.getId());
        entityManager.clear();

        List<SecretariaInstitucionalItem> depoisDoFiltro = itemRepository.findAll();

        assertThat(depoisDoFiltro).extracting(SecretariaInstitucionalItem::getId).contains(itemX.getId());
        assertThat(depoisDoFiltro).extracting(SecretariaInstitucionalItem::getId).doesNotContain(itemY.getId());
        assertThat(depoisDoFiltro).extracting(SecretariaInstitucionalItem::getUnidadeInstitucionalId)
                .containsOnly(unidadeX.getId());
    }

    private UnidadeInstituicao novaUnidade(Instituicao instituicao, String nome, String comarca) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome(nome);
        unidade.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        unidade.setComarca(comarca);
        unidade.setUf("CE");
        return unidadeRepository.save(unidade);
    }

    private Processo novoProcesso(String numero) {
        return processoRepository.save(Processo.builder()
                .numeroProcesso(numero)
                .numeroUnificado(numero)
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.PENAL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build());
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
}
