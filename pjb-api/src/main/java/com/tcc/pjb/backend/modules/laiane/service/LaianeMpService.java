package com.tcc.pjb.backend.modules.laiane.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaEventoComportamental;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaRepository;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.*;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;
import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeOficioRepository;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LaianeMpService {

    private final LaianeRoleGuard guard;
    private final WorkItemRepository workItemRepository;
    private final LaianeOficioRepository oficioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaInteligenteService auditoria;
    private final AuditoriaRepository auditoriaRepository;
    private final PjbTimeService timeService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    
    
    

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
        var mp = guard.requireMinisterioPublico();

        Usuario destino = null;
        if (req.getDestinoId() != null) {
            destino = usuarioRepository.findById(req.getDestinoId())
                    .orElseThrow(() -> new NoSuchElementException("Destino não encontrado"));
        }

        LaianeOficio oficio = LaianeOficio.builder()
                .trackingCode(UUID.randomUUID())
                .status(LaianeOficioStatus.CRIADO)
                .origem(mp)
                .destino(destino)
                .tipo(req.getTipo())
                .protocolo(req.getProtocolo())
                .assunto(req.getAssunto())
                .conteudo(req.getConteudo())
                .build();

        oficio = oficioRepository.save(oficio);

        auditoria.registrarEventoImutavelJustificado(
                "MP_OFICIO_CRIADO",
                String.valueOf(oficio.getTrackingCode()),
                "tipo=" + req.getTipo() + ";destinoId=" + (destino != null ? destino.getId() : null),
                req.getJustificativa()
        );

        return toOficioResponse(oficio);
    }

    @Transactional(readOnly = true)
    public LaianeMpOficioResponse getOficio(UUID trackingCode) {
        guard.requireMinisterioPublico();
        LaianeOficio oficio = oficioRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new NoSuchElementException("Ofício não encontrado"));
        return toOficioResponse(oficio);
    }

    @Transactional
    public LaianeMpOficioResponse updateOficioStatus(UUID trackingCode, LaianeMpOficioStatusUpdateRequest req) {
        var mp = guard.requireMinisterioPublico();
        LaianeOficio oficio = oficioRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new NoSuchElementException("Ofício não encontrado"));

        LaianeOficioStatus status = LaianeOficioStatus.from(req.getStatus());
        oficio.setStatus(status);
        if (status == LaianeOficioStatus.ENVIADO) {
            oficio.setEnviadoEm(LocalDateTime.now());
        }
        if (status == LaianeOficioStatus.ENTREGUE) {
            oficio.setEntregueEm(LocalDateTime.now());
        }

        oficio = oficioRepository.save(oficio);

        auditoria.registrarEventoImutavelJustificado(
                "MP_OFICIO_STATUS",
                String.valueOf(oficio.getTrackingCode()),
                "status=" + status.name() + ";mpId=" + mp.getId(),
                req.getJustificativa()
        );

        return toOficioResponse(oficio);
    }

    @Transactional(readOnly = true)
    public LaianeMpAuditResponse audit(String referenciaId, String acao, Long usuarioId, int page, int size) {
        guard.requireMinisterioPublico();

        int safePage = Math.max(0, page);
        int safeSize = Math.max(5, Math.min(size, 200));

        var result = auditoriaRepository.search(referenciaId, acao, usuarioId, PageRequest.of(safePage, safeSize));

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
        Map<String, Object> documentoFormalAssinado = buildSignedOficio(o);
        @SuppressWarnings("unchecked")
        Map<String, Object> assinaturaQualificada = documentoFormalAssinado == null ? Map.of() : (Map<String, Object>) documentoFormalAssinado.getOrDefault("assinaturaQualificada", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> validacaoSoberana = documentoFormalAssinado == null ? Map.of() : (Map<String, Object>) documentoFormalAssinado.getOrDefault("validacaoSoberana", Map.of());
        return LaianeMpOficioResponse.builder()
                .id(o.getId())
                .trackingCode(o.getTrackingCode())
                .origemId(o.getOrigem() != null ? o.getOrigem().getId() : null)
                .destinoId(o.getDestino() != null ? o.getDestino().getId() : null)
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

    private Map<String, Object> buildSignedOficio(LaianeOficio oficio) {
        if (oficio == null) {
            return Map.of();
        }
        Usuario actor = oficio.getOrigem();
        String titulo = firstNonBlank(oficio.getAssunto(), oficio.getTipo(), "Ofício do Ministério Público");
        String conteudo = materializeOficioContent(oficio);
        QualifiedDocumentSignatureEnvelopeService.SignedContent signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null,
                actor,
                titulo,
                conteudo,
                resolvePaper(actor),
                "MINISTERIO_PUBLICO_QUALIFICADA_SOBERANA",
                true,
                List.of("MINISTERIO_PUBLICO", "OFICIO_INSTITUCIONAL", "LAIANE_MP")
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("tituloDocumento", titulo);
        out.put("conteudoAssinado", signedContent.renderedContent());
        out.put("hashSha256", signedContent.contentHash());
        out.put("assinaturaQualificada", signedContent.assinaturaQualificada());
        out.put("validacaoSoberana", signedContent.validacaoSoberana());
        out.put("selado", Boolean.TRUE);
        return Map.copyOf(out);
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
