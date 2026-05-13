package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;

@Service
public class InstitutionalNoReadCertificationApplicationService {

    private final InstitutionalInboxStateRepository inboxRepository;
    private final InstitutionalCommunicationAuditApplicationService auditService;
    private final CurrentUserService currentUserService;

    public InstitutionalNoReadCertificationApplicationService(InstitutionalInboxStateRepository inboxRepository,
                                                              InstitutionalCommunicationAuditApplicationService auditService,
                                                              CurrentUserService currentUserService) {
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.auditService = Objects.requireNonNull(auditService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public InstitutionalInboxItem certificarNaoLeitura(String expedicaoUuid, String detalhe) {
        InstitutionalInboxItem item = inboxRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new IllegalArgumentException("expedicao institucional não encontrada"));
        if (item.status() == StatusComunicacaoInstitucional.CUMPRIDA || item.status() == StatusComunicacaoInstitucional.CIENTIFICADA) {
            throw new IllegalStateException("expedição já possui ciência/cumprimento");
        }
        if (item.prazoCienciaEm() != null && item.prazoCienciaEm().isAfter(Instant.now())) {
            throw new IllegalStateException("prazo de ciência ainda não expirou");
        }
        Usuario actor = currentUserService.getRequired();
        auditService.registrarCertidaoNaoLeitura(item, actor, detalhe == null || detalhe.isBlank() ? "Decurso de prazo sem leitura institucional." : detalhe.trim());
        if (item.status() == StatusComunicacaoInstitucional.DISPONIBILIZADA) {
            auditService.registrarAvisoExterno(item, "EMAIL_AVISO", "Aviso acessório disparado após não leitura institucional.");
        }
        return item;
    }

    public List<InstitutionalInboxItem> pendentesDecurso(String unidadeCodigo) {
        return inboxRepository.findPendentesDecurso(unidadeCodigo, Instant.now()).stream()
                .filter(item -> item.status() == StatusComunicacaoInstitucional.DISPONIBILIZADA || item.status() == StatusComunicacaoInstitucional.RECEBIDA)
                .toList();
    }
}
