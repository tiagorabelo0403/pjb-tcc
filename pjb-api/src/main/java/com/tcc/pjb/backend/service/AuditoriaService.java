package com.tcc.pjb.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.AuditoriaEvento;
import com.tcc.pjb.backend.model.repository.AuditoriaEventoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaEventoRepository repository;
    private final AuditLedgerService auditLedgerService;

    
    public void registrar(String usuario, String acao, String alvo) {
        try {
            AuditoriaEvento saved = repository.save(AuditoriaEvento.builder()
                    .usuario(usuario)
                    .acao(acao == null ? "(sem_acao)" : acao)
                    .alvo(alvo)
                    .build());

            
            auditLedgerService.append(
                    saved.getAcao(),
                    "AuditoriaOperacional",
                    saved.getId() == null ? (alvo == null ? "(sem_alvo)" : alvo) : String.valueOf(saved.getId()),
                    null,
                    RequestContext.getJustificativa().orElse(null)
            );
        } catch (Exception e) {
            
            log.warn("Falha ao registrar auditoria operacional: usuario={} acao={} alvo={} err={}",
                    usuario, acao, alvo, e.getMessage());
        }
    }
}
