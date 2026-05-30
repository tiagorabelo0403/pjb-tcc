package com.tcc.pjb.backend.modules.laiane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.modules.laiane.event.LaianeOficioAuditEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.ValidationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaEventoComportamental;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaRepository;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.*;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;
import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeOficioRepository;
import com.tcc.pjb.backend.modules.laiane.security.LaianeOficioAccessGuard;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import com.tcc.pjb.backend.core.security.audit.PjbSecurityEventLogger;
@Service
public class LaianeMpService {

    private final LaianeRoleGuard guard;
    private final LaianeOficioAccessGuard accessGuard;
    private final WorkItemRepository workItemRepository;
    private final LaianeOficioRepository oficioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final PjbTimeService timeService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final CapabilityRateLimiter capabilityRateLimiter;
    private final PjbSecurityEventLogger securityEventLogger;

    public LaianeMpService(LaianeRoleGuard guard,
                           LaianeOficioAccessGuard accessGuard,
                           WorkItemRepository workItemRepository,
                           LaianeOficioRepository oficioRepository,
                           UsuarioRepository usuarioRepository,
                           AuditoriaRepository auditoriaRepository,
                           PjbTimeService timeService,
                           QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService,
                           ObjectMapper objectMapper,
                           ApplicationEventPublisher eventPublisher,
                           MeterRegistry meterRegistry,
                           CapabilityRateLimiter capabilityRateLimiter,
                           PjbSecurityEventLogger securityEventLogger) {
        this.guard = guard;
        this.accessGuard = accessGuard;
        this.workItemRepository = workItemRepository;
        this.oficioRepository = oficioRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.timeService = timeService;
        this.qualifiedDocumentSignatureEnvelopeService = qualifiedDocumentSignatureEnvelopeService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
        this.capabilityRateLimiter = capabilityRateLimiter;
        this.securityEventLogger = securityEventLogger;
    }

    
    
    

    public LaianeMpInboxResponse inbox() {
        var mp = guard.requireMinisterioPublico();
        Page<WorkItem> page = workItemRepository.inboxByUser(mp.getId(), PageRequest.of(0, 30));
        return LaianeMpInboxResponse.builder()
                .items(page.getContent().stream().map(this::toLite).toList())
                .hint("Inbox MVP: priorize por prazo. Próximo: monitor de prazos + ofícios.")
                .build();
    }

    
    
    

    @Transactional(readOnly = true)
    public LaianeMpDeadlineMonitorResponse monitorDeadlines(int horizonDays, int limit) {
        var mp = guard.requireMinisterioPublico();
        int safeDays = Math.max(1, Math.min(horizonDays, 90));
        int safeLimit = Math.max(5, Math.min(limit, 200));

        Instant now = timeService.nowUtc();
        Instant until = now.plus(safeDays, ChronoUnit.DAYS);

        List<WorkItem> due = workItemRepository.findDueByAssignedUser(mp.getId(), until, PageRequest.of(0, safeLimit));

        long overdue = due.stream().filter(w -> w.getDueAt() != null && w.getDueAt().isBefore(now)).count();
        long upcoming = due.size() - overdue;

        return LaianeMpDeadlineMonitorResponse.builder()
                .generatedAt(now)
                .horizonDays(safeDays)
                .total(due.size())
                .overdue((int) overdue)
                .upcoming((int) upcoming)
                .items(due.stream().map(this::toLite).toList())
                .build();
    }

    @Transactional
    public LaianeMpOficioResponse createOficio(LaianeMpOficioCreateRequest req) {
        if (capabilityRateLimiter != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            capabilityRateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, auth, "laiane_mp_oficio_create", ApiVersion.V1);
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        var mp = guard.requireMinisterioPublico();

        Usuario destino = null;
        if (req.getDestinoId() != null) {
            destino = usuarioRepository.findById(req.getDestinoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destino não encontrado"));
        }

        String bodyHash = computeBodyHash(req);
        if (bodyHash != null) {
            LocalDateTime since = LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone()).minusMinutes(5);
            Optional<LaianeOficio> existing = oficioRepository.findFirstByBodyHashAndCreatedAtAfter(bodyHash, since);
            if (existing.isPresent()) {
                securityEventLogger.idempotencyHit(bodyHash,
                        existing.get().getTrackingCode() != null ? existing.get().getTrackingCode().toString() : "DESCONHECIDO");
                return toOficioResponse(existing.get());
            }
        }

        LaianeOficio oficio = LaianeOficio.builder()
                .trackingCode(PjbUuidV7Generator.generate())
                .status(LaianeOficioStatus.CRIADO)
                .origem(mp)
                .destino(destino)
                .tipo(req.getTipo())
                .protocolo(req.getProtocolo())
                .assunto(req.getAssunto())
                .conteudo(req.getConteudo())
                .bodyHash(bodyHash)
                .build();

        oficio.setSignedEnvelopeJson(serializeSignedEnvelope(oficio));
        oficio = oficioRepository.save(oficio);

        eventPublisher.publishEvent(new LaianeOficioAuditEvent(
                "MP_OFICIO_CRIADO",
                String.valueOf(oficio.getTrackingCode()),
                "tipo=" + req.getTipo() + ";destinoId=" + (destino != null ? destino.getId() : null),
                req.getJustificativa()
        ));

        sample.stop(Timer.builder("pjb.laiane.mp.oficio.criado")
                .tag("tipo", req.getTipo() != null ? req.getTipo() : "DESCONHECIDO")
                .register(meterRegistry));
        meterRegistry.counter("pjb.laiane.mp.oficio.total", "operacao", "criar").increment();

        return toOficioResponse(oficio);
    }

    @Transactional(readOnly = true)
    public LaianeMpOficioResponse getOficio(UUID trackingCode) {
        meterRegistry.counter("pjb.laiane.mp.oficio.consulta").increment();
        Usuario mp = guard.requireMinisterioPublico();
        LaianeOficio oficio = findOficioForMp(trackingCode, mp.getId());
        accessGuard.requireRead(oficio);
        return toOficioResponse(oficio);
    }

    @Transactional
    public LaianeMpOficioResponse updateOficioStatus(UUID trackingCode, LaianeMpOficioStatusUpdateRequest req) {
        var mp = guard.requireMinisterioPublico();
        LaianeOficio oficio = findOficioForMp(trackingCode, mp.getId());
        accessGuard.requireUpdateStatus(oficio);

        LaianeOficioStatus currentStatus = oficio.getStatus();
        LaianeOficioStatus targetStatus;
        try {
            targetStatus = LaianeOficioStatus.valueOf(req.getStatus());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status desconhecido: " + req.getStatus());
        }
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transição inválida: " + currentStatus + " → " + targetStatus);
        }
        accessGuard.requireHighAssuranceForCancellation(targetStatus);
        oficio.setStatus(targetStatus);
        if (targetStatus == LaianeOficioStatus.ENVIADO) {
            oficio.setEnviadoEm(LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone()));
        }
        if (targetStatus == LaianeOficioStatus.ENTREGUE) {
            oficio.setEntregueEm(LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone()));
        }

        oficio = oficioRepository.save(oficio);

        eventPublisher.publishEvent(new LaianeOficioAuditEvent(
                "MP_OFICIO_STATUS",
                String.valueOf(oficio.getTrackingCode()),
                "status=" + targetStatus.name() + ";mpId=" + mp.getId(),
                req.getJustificativa()
        ));
        meterRegistry.counter("pjb.laiane.mp.oficio.status.transicao",
                "de", currentStatus.name(), "para", targetStatus.name()).increment();

        return toOficioResponse(oficio);
    }

    private LaianeOficio findOficioForMp(UUID trackingCode, Long usuarioId) {
        return oficioRepository.findByTrackingCodeAndUsuarioEnvolvido(trackingCode, usuarioId)
                .or(() -> oficioRepository.findByTrackingCode(trackingCode))
                .orElseThrow(() -> new NoSuchElementException("Ofício não encontrado"));
    }

    @Transactional(readOnly = true)
    public LaianeMpAuditResponse audit(String referenciaId, String acao, int page, int size) {
        meterRegistry.counter("pjb.laiane.mp.auditoria.consulta").increment();
        Usuario me = guard.requireMinisterioPublico();

        int safePage = Math.max(0, page);
        int safeSize = Math.max(5, Math.min(size, 200));

        var result = auditoriaRepository.search(referenciaId, acao, me.getId(), PageRequest.of(safePage, safeSize));

        List<LaianeMpAuditEventDto> items = result.getContent().stream().map(this::toAuditDto).toList();

        return LaianeMpAuditResponse.builder()
                .page(safePage)
                .size(safeSize)
                .total(result.getTotalElements())
                .items(items)
                .build();
    }

    private LaianeWorkItemLiteDto toLite(WorkItem w) {
        return LaianeWorkItemLiteDto.builder()
                .id(w.getId())
                .processoId(w.getProcesso() != null ? w.getProcesso().getId() : null)
                .titulo(w.getTitulo())
                .status(w.getStatus() != null ? w.getStatus().name() : null)
                .prioridade(w.getPrioridade())
                .blocking(w.isBlocking())
                .dueAt(w.getDueAt())
                .build();
    }

    private LaianeMpOficioResponse toOficioResponse(LaianeOficio o) {
        LaianeDocumentoFormalAssinadoResponse documentoFormalAssinado = buildSignedOficio(o);
        LaianeAssinaturaQualificadaResponse assinaturaQualificada = documentoFormalAssinado == null ? null : documentoFormalAssinado.assinaturaQualificada();
        LaianeValidacaoSoberanaResponse validacaoSoberana = documentoFormalAssinado == null ? null : documentoFormalAssinado.validacaoSoberana();
        return LaianeMpOficioResponse.builder()
                .trackingCode(o.getTrackingCode())
                .status(o.getStatus() != null ? o.getStatus().name() : null)
                .tipo(o.getTipo())
                .protocolo(o.getProtocolo())
                .assunto(o.getAssunto())
                .conteudo(o.getConteudo())
                .documentoFormalAssinado(documentoFormalAssinado)
                .assinaturaQualificada(assinaturaQualificada)
                .validacaoSoberana(validacaoSoberana)
                .enviadoEm(o.getEnviadoEm())
                .entregueEm(o.getEntregueEm())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private LaianeDocumentoFormalAssinadoResponse buildSignedOficio(LaianeOficio oficio) {
        if (oficio == null) return null;
        if (oficio.getSignedEnvelopeJson() != null) {
            try {
                SignedDocumentEnvelope env = objectMapper.readValue(
                        oficio.getSignedEnvelopeJson(), SignedDocumentEnvelope.class);
                return toDocumentoFormalAssinado(env);
            } catch (Exception ignored) { }
        }
        return toDocumentoFormalAssinado(signOficio(oficio));
    }

    private SignedDocumentEnvelope signOficio(LaianeOficio oficio) {
        Usuario actor = oficio.getOrigem();
        String titulo = firstNonBlank(oficio.getAssunto(), oficio.getTipo(), "Ofício do Ministério Público");
        String conteudo = materializeOficioContent(oficio);
        return qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null, actor, titulo, conteudo, resolvePaper(actor),
                "MINISTERIO_PUBLICO_QUALIFICADA_SOBERANA", true,
                List.of("MINISTERIO_PUBLICO", "OFICIO_INSTITUCIONAL", "LAIANE_MP"));
    }

    private String computeBodyHash(LaianeMpOficioCreateRequest req) {
        try {
            return Hashes.sha256Hex(objectMapper.writeValueAsString(req));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String serializeSignedEnvelope(LaianeOficio oficio) {
        try {
            SignedDocumentEnvelope envelope = signOficio(oficio);
            if (envelope == null) return null;
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LaianeDocumentoFormalAssinadoResponse toDocumentoFormalAssinado(SignedDocumentEnvelope source) {
        if (source == null) {
            return null;
        }
        LaianeValidacaoSoberanaResponse validacaoSoberana = toValidacaoSoberana(source.validacaoSoberana());
        LaianeAssinaturaQualificadaResponse assinaturaQualificada = toAssinaturaQualificada(source.assinaturaQualificada(), validacaoSoberana);
        return new LaianeDocumentoFormalAssinadoResponse(
                source.tituloDocumento(),
                source.renderedContent(),
                source.contentHash(),
                source.selado(),
                assinaturaQualificada,
                validacaoSoberana
        );
    }

    private LaianeAssinaturaQualificadaResponse toAssinaturaQualificada(QualifiedSignatureMetadata source, LaianeValidacaoSoberanaResponse validacaoSoberana) {
        if (source == null) {
            return null;
        }
        return new LaianeAssinaturaQualificadaResponse(
                source.envelopeId(),
                source.assinaturaHash(),
                source.conteudoBaseHash(),
                source.documentoAssinadoHash(),
                source.rubrica(),
                source.data(),
                source.hora(),
                source.local(),
                source.signatario(),
                source.papelAssinante(),
                source.papelAssinanteDetalhado(),
                source.segmentoInstitucional(),
                source.ramoJustica(),
                source.esferaInstitucional(),
                source.instancia(),
                source.orgaoAssinante(),
                source.lotacaoAssinante(),
                source.registroProfissional(),
                source.coerenciaCertificadoUsuario(),
                source.sessionBindingHash(),
                source.replayShieldHash(),
                validacaoSoberana
        );
    }

    private LaianeValidacaoSoberanaResponse toValidacaoSoberana(SovereignValidationResult source) {
        if (source == null) {
            return null;
        }
        return new LaianeValidacaoSoberanaResponse(
                source.status(),
                source.fonte(),
                source.politicaAssinatura(),
                source.cadeiaCustodiaElegivel(),
                source.assinaturaCompletaMaterializada(),
                source.rubricaDataHoraLocalPresentes(),
                source.classificacaoContextualCoerente(),
                source.certificadoEntradaVinculado(),
                source.papelAssinanteDetalhado(),
                source.ramoJustica(),
                source.instancia(),
                source.lotacaoAssinante(),
                source.sessionBindingHash(),
                source.replayShieldHash(),
                source.documentoAssinadoHash(),
                source.regrasAplicadas() == null ? List.of() : source.regrasAplicadas().stream().map(this::toRegraValidacao).toList()
        );
    }

    private LaianeRegraValidacaoResponse toRegraValidacao(ValidationRule source) {
        return new LaianeRegraValidacaoResponse(source.codigo(), source.descricao(), source.resultado());
    }

    private String materializeOficioContent(LaianeOficio oficio) {
        String destino = oficio.getDestino() == null || oficio.getDestino().getNome() == null || oficio.getDestino().getNome().isBlank()
                ? "DESTINATARIO_NAO_INFORMADO"
                : oficio.getDestino().getNome().trim();
        String protocolo = oficio.getProtocolo() == null || oficio.getProtocolo().isBlank()
                ? "PROTOCOLO_NAO_INFORMADO"
                : oficio.getProtocolo().trim();
        String tipo = oficio.getTipo() == null || oficio.getTipo().isBlank()
                ? "OFICIO_INSTITUCIONAL"
                : oficio.getTipo().trim();
        String assunto = oficio.getAssunto() == null || oficio.getAssunto().isBlank()
                ? "ASSUNTO_NAO_INFORMADO"
                : oficio.getAssunto().trim();
        String conteudo = oficio.getConteudo() == null || oficio.getConteudo().isBlank()
                ? "CONTEUDO_NAO_INFORMADO"
                : oficio.getConteudo().trim();
        return String.join(System.lineSeparator(),
                "Ofício institucional do Ministério Público",
                "Tipo: " + tipo,
                "Protocolo: " + protocolo,
                "Assunto: " + assunto,
                "Destinatário: " + destino,
                "",
                conteudo);
    }

    private String resolvePaper(Usuario actor) {
        if (actor == null || actor.getTipoUsuario() == null) {
            return "MINISTERIO_PUBLICO";
        }
        return actor.getTipoUsuario().name();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "DOCUMENTO_INSTITUCIONAL";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "DOCUMENTO_INSTITUCIONAL";
    }

    private LaianeMpAuditEventDto toAuditDto(AuditoriaEventoComportamental e) {
        return LaianeMpAuditEventDto.builder()
                .uuid(e.getUuid())
                .acao(e.getAcao())
                .usuarioId(e.getUsuarioId())
                .referenciaId(e.getReferenciaId())
                .detalhes(e.getDetalhes())
                .justificativa(e.getJustificativa())
                .timestamp(e.getTimestamp())
                .nivelRisco(String.valueOf(e.getNivelRisco()))
                .perfilComportamental(e.getPerfilComportamental())
                .hashIntegridade(e.getHashIntegridade())
                .build();
    }
}
