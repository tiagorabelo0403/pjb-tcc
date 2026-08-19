package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.dto.secretariat.SecretariaInstitucionalFilaResponse;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariaInstitucionalFilaService {

    private static final String SESSION_VAR_UNIDADE_ID = "app.pjb_secretaria_unidade_id";
    private static final String HIBERNATE_FILTER_UNIDADE = "filtroUnidadeInstitucional";
    private static final String HIBERNATE_FILTER_PARAM_UNIDADE_ID = "unidadeInstitucionalIdParam";

    private final UnidadeInstituicaoRepository unidadeRepository;
    private final SecretariaInstitucionalItemRepository itemRepository;
    private final UnidadeInstitucionalVisibilityPolicy visibilityPolicy;
    private final EntityManager entityManager;

    public SecretariaInstitucionalFilaService(UnidadeInstituicaoRepository unidadeRepository,
                                              SecretariaInstitucionalItemRepository itemRepository,
                                              UnidadeInstitucionalVisibilityPolicy visibilityPolicy,
                                              EntityManager entityManager) {
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.visibilityPolicy = Objects.requireNonNull(visibilityPolicy);
        this.entityManager = Objects.requireNonNull(entityManager);
    }

    @Transactional(readOnly = true)
    public SecretariaInstitucionalFilaResponse consultarFila(Usuario usuario, Long unidadeId) {
        UnidadeInstituicao unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada: " + unidadeId));
        if (!visibilityPolicy.podeVer(usuario, unidade)) {
            throw new SecurityException("Usuário não tem visibilidade sobre esta unidade institucional");
        }
        aplicarEscopoDeSessao(unidadeId);
        ativarFiltroHibernateDeUnidade(unidadeId);
        List<SecretariaInstitucionalItem> itens = itemRepository.findByUnidadeInstitucionalIdOrderByPrazoFatalAsc(unidadeId);
        List<SecretariaInstitucionalFilaResponse.Item> itensResponse = itens.stream()
                .map(i -> new SecretariaInstitucionalFilaResponse.Item(i.getId(), i.getProcessoId(),
                        i.getStatus().name(), i.getMotivo().name(), i.getPrazoFatal(), i.isPrazoEmDobro(),
                        i.getIntimadoEm(), i.getIntimacaoTacitaEm()))
                .toList();
        return new SecretariaInstitucionalFilaResponse(unidadeId, unidade.getNome(),
                unidade.getTipo() == null ? null : unidade.getTipo().name(), unidade.getComarca(), itensResponse);
    }

    private void aplicarEscopoDeSessao(Long unidadeId) {
        entityManager.createNativeQuery("SELECT set_config('" + SESSION_VAR_UNIDADE_ID + "', ?1, true)")
                .setParameter(1, unidadeId.toString())
                .getSingleResult();
    }

    private void ativarFiltroHibernateDeUnidade(Long unidadeId) {
        entityManager.unwrap(Session.class)
                .enableFilter(HIBERNATE_FILTER_UNIDADE)
                .setParameter(HIBERNATE_FILTER_PARAM_UNIDADE_ID, unidadeId);
    }
}
