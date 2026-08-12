package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.dto.secretariat.SecretariaInstitucionalFilaResponse;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariaInstitucionalFilaService {

    private final UnidadeInstituicaoRepository unidadeRepository;
    private final SecretariaInstitucionalItemRepository itemRepository;
    private final UnidadeInstitucionalVisibilityPolicy visibilityPolicy;

    public SecretariaInstitucionalFilaService(UnidadeInstituicaoRepository unidadeRepository,
                                              SecretariaInstitucionalItemRepository itemRepository,
                                              UnidadeInstitucionalVisibilityPolicy visibilityPolicy) {
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.visibilityPolicy = Objects.requireNonNull(visibilityPolicy);
    }

    @Transactional(readOnly = true)
    public SecretariaInstitucionalFilaResponse consultarFila(Usuario usuario, Long unidadeId) {
        UnidadeInstituicao unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada: " + unidadeId));
        if (!visibilityPolicy.podeVer(usuario, unidade)) {
            throw new SecurityException("Usuário não tem visibilidade sobre esta unidade institucional");
        }
        List<SecretariaInstitucionalItem> itens = itemRepository.findByUnidadeInstitucionalIdOrderByPrazoFatalAsc(unidadeId);
        List<SecretariaInstitucionalFilaResponse.Item> itensResponse = itens.stream()
                .map(i -> new SecretariaInstitucionalFilaResponse.Item(i.getId(), i.getProcessoId(),
                        i.getStatus().name(), i.getMotivo().name(), i.getPrazoFatal(), i.isPrazoEmDobro(),
                        i.getIntimadoEm(), i.getIntimacaoTacitaEm()))
                .toList();
        return new SecretariaInstitucionalFilaResponse(unidadeId, itensResponse);
    }
}
