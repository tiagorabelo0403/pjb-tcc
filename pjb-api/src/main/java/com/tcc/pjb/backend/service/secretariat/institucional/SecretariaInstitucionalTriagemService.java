package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.dto.secretariat.SecretariaInstitucionalItemResponse;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triagem administrativa dos itens que o roteamento institucional não conseguiu atribuir a nenhuma
 * unidade. Separado de {@link SecretariaInstitucionalFilaService} de propósito: aquele responde pela
 * fila de uma unidade, com visibilidade do usuário e filtro de sessão por unidade; este responde pelo
 * resíduo sem unidade, que por definição fica fora daquele escopo.
 */
@Service
public class SecretariaInstitucionalTriagemService {

    private final SecretariaInstitucionalItemRepository itemRepository;

    public SecretariaInstitucionalTriagemService(SecretariaInstitucionalItemRepository itemRepository) {
        this.itemRepository = Objects.requireNonNull(itemRepository);
    }

    @Transactional(readOnly = true)
    public List<SecretariaInstitucionalItemResponse> itensSemUnidadeResolvida() {
        return itemRepository.findByStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA)
                .stream()
                .map(SecretariaInstitucionalItemResponse::de)
                .toList();
    }
}
