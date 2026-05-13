package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVector;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalRecentAuditDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalProcessViewAuditEvent;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalProcessViewAuditEventRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalProcessViewAuditService {

    private final ProfessionalProcessViewAuditEventRepository repository;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;

    public ProfessionalProcessViewAuditService(ProfessionalProcessViewAuditEventRepository repository,
                                               CurrentUserService currentUserService,
                                               AuditLedgerService auditLedgerService) {
        this.repository = Objects.requireNonNull(repository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public void recordSearch(String queryType,
                             String queryValue,
                             ProfessionalProcessAccessVector vector,
                             boolean success,
                             String fingerprint) {
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getId() == null || vector == null) {
            return;
        }
        ProfessionalProcessViewAuditEvent event = new ProfessionalProcessViewAuditEvent();
        event.setUsuarioId(usuario.getId());
        event.setUsuarioNome(safe(usuario.getNome()));
        event.setOabOuMatricula(resolveProfessionalRegistration(usuario));
        event.setProcessoId(0L);
        event.setNumeroProcesso("BUSCA_PROFISSIONAL");
        event.setActorClass(vector.actorClass().name());
        event.setPanelMode(vector.panelMode());
        event.setAccessBasis(vector.primaryBasis().name());
        event.setOperationType("SEARCH");
        event.setQueryType(queryType);
        event.setQueryValueMasked(maskQuery(queryType, queryValue));
        event.setReason(vector.reason());
        event.setStepUpMode(vector.requiresStepUp() ? "STEP_UP_REQUIRED" : "NONE");
        event.setSucesso(success);
        event.setClientFingerprintHash(hash(fingerprint));
        event.setAcessadoEm(LocalDateTime.now());
        repository.save(event);
        auditLedgerService.appendSafely("PROFESSIONAL_SEARCH", "PROFESSIONAL_PANEL", usuario.getId().toString(),
                "{\"queryType\":\"" + safe(queryType) + "\",\"success\":" + success + "}");
    }

    @Transactional
    public void recordProcessView(Processo processo,
                                  String documentoId,
                                  ProfessionalProcessAccessVector vector,
                                  String queryType,
                                  String queryValue,
                                  boolean success,
                                  String fingerprint) {
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getId() == null || processo == null || processo.getId() == null || vector == null) {
            return;
        }
        ProfessionalProcessViewAuditEvent event = new ProfessionalProcessViewAuditEvent();
        event.setUsuarioId(usuario.getId());
        event.setUsuarioNome(safe(usuario.getNome()));
        event.setOabOuMatricula(resolveProfessionalRegistration(usuario));
        event.setProcessoId(processo.getId());
        event.setNumeroProcesso(resolveNumero(processo));
        event.setDocumentoId(documentoId);
        event.setActorClass(vector.actorClass().name());
        event.setPanelMode(vector.panelMode());
        event.setAccessBasis(vector.primaryBasis().name());
        event.setOperationType(documentoId == null || documentoId.isBlank() ? "PROCESS_VIEW" : "DOCUMENT_VIEW");
        event.setQueryType(queryType);
        event.setQueryValueMasked(maskQuery(queryType, queryValue));
        event.setReason(vector.reason());
        event.setStepUpMode(vector.requiresStepUp() ? "STEP_UP_REQUIRED" : "NONE");
        event.setSucesso(success);
        event.setClientFingerprintHash(hash(fingerprint));
        event.setAcessadoEm(LocalDateTime.now());
        repository.save(event);
        auditLedgerService.appendSafely("PROFESSIONAL_PROCESS_VIEW", "PROFESSIONAL_PANEL", String.valueOf(processo.getId()),
                "{\"numero\":\"" + resolveNumero(processo) + "\",\"actorClass\":\"" + vector.actorClass().name() + "\"}");
    }

    @Transactional(readOnly = true)
    public List<ProfessionalRecentAuditDto> recentForCurrentUser() {
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getId() == null) {
            return List.of();
        }
        return repository.findTop20ByUsuarioIdOrderByAcessadoEmDesc(usuario.getId()).stream()
                .map(item -> new ProfessionalRecentAuditDto(
                        item.getId(),
                        item.getAcessadoEm(),
                        item.getOperationType(),
                        item.getProcessoId(),
                        item.getNumeroProcesso(),
                        item.getActorClass(),
                        item.getAccessBasis(),
                        item.getQueryType(),
                        item.getQueryValueMasked(),
                        item.isSucesso(),
                        item.getReason()
                ))
                .toList();
    }

    private String resolveProfessionalRegistration(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        if (usuario.getOab() != null && !usuario.getOab().isBlank()) {
            return usuario.getOab();
        }
        if (usuario.getPerfil() != null && !usuario.getPerfil().isBlank()) {
            return usuario.getPerfil();
        }
        return usuario.getEmail();
    }

    private String resolveNumero(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        return processo.getNumeroProcesso();
    }

    private String maskQuery(String queryType, String queryValue) {
        if (queryValue == null || queryValue.isBlank()) {
            return null;
        }
        if ("CPF".equalsIgnoreCase(queryType) || "CNPJ".equalsIgnoreCase(queryType)) {
            String digits = queryValue.replaceAll("\\D", "");
            if (digits.length() <= 4) {
                return "***";
            }
            return "***" + digits.substring(Math.max(0, digits.length() - 4));
        }
        if (queryValue.length() <= 10) {
            return queryValue;
        }
        return queryValue.substring(0, 10) + "...";
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
