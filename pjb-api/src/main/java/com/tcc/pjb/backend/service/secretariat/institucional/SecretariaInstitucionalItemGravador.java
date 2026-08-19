package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SecretariaInstitucionalItemGravador {

    private final SecretariaInstitucionalItemRepository itemRepository;

    public SecretariaInstitucionalItemGravador(SecretariaInstitucionalItemRepository itemRepository) {
        this.itemRepository = Objects.requireNonNull(itemRepository);
    }

    // REQUIRES_NEW isola o insert em transação física própria, mas não catch DataIntegrityViolationException
    // aqui dentro: o flush já marca ESTA transação como rollback-only no momento do conflito do índice
    // único (Task 2), então um catch neste método ainda derrubaria o commit dele com UnexpectedRollbackException.
    // A exceção precisa atravessar esta borda @Transactional sem ser tratada — assim o Spring faz rollback
    // limpo (não commit) e relança a exceção original, que só então é segura para o chamador capturar,
    // porque nesse ponto a transação REQUIRES_NEW já terminou e não contamina a transação ambiente dele.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecretariaInstitucionalItem gravar(SecretariaInstitucionalItem item) {
        return itemRepository.save(item);
    }
}
