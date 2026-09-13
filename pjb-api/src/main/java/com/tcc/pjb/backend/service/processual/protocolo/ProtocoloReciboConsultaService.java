package com.tcc.pjb.backend.service.processual.protocolo;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.protocolo.ProtocoloReciboResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve o processo e decide se quem pede pode lê-lo, antes de pedir o recibo a
 * {@link ProtocoloReciboService}. Separado dele de propósito: aquele responde por emitir e recuperar
 * o documento de recibo, este responde por quem tem direito de vê-lo. A negativa sai como
 * {@link SecurityException}, que o tratador de exceções da API converte em 403 com corpo genérico —
 * o motivo da negativa fica no domínio e não volta para quem foi negado, porque a própria razão pode
 * revelar o que o sigilo protege.
 */
@Service
public class ProtocoloReciboConsultaService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProtocoloReciboService protocoloReciboService;

    public ProtocoloReciboConsultaService(ProcessoRepository processoRepository,
                                          PjbAuthorizationService authorizationService,
                                          ProtocoloReciboService protocoloReciboService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.protocoloReciboService = Objects.requireNonNull(protocoloReciboService);
    }

    @Transactional
    public ProtocoloReciboResponse reciboDoProcesso(Long processoId, Usuario solicitante) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        AuthzDecision decisao = authorizationService.canReadProcesso(processo);
        if (!decisao.allowed()) {
            throw new SecurityException(decisao.reason());
        }
        return protocoloReciboService.obterOuEmitirRecibo(processo, solicitante);
    }
}
