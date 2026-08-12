package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecretariaInstitucionalAutoEnfileiramentoListener {

    private static final int PRAZO_BASE_DIAS_PARTE_AUTOMATICA = 15;

    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService;

    public SecretariaInstitucionalAutoEnfileiramentoListener(SecretariaInstitucionalEnfileiramentoService enfileiramentoService) {
        this.enfileiramentoService = Objects.requireNonNull(enfileiramentoService);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoComporPoloInstitucional(PoloInstitucionalComposicaoEvent evento) {
        enfileiramentoService.enfileirar(evento.processoId(), evento.comarca(), evento.tipo(),
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, PRAZO_BASE_DIAS_PARTE_AUTOMATICA);
    }
}
