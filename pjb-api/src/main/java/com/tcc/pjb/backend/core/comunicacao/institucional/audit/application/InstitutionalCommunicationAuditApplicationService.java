package com.tcc.pjb.backend.core.comunicacao.institucional.audit.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalDeliveryProof;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalTimelineEvent;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalTimelineEventType;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.infrastructure.InstitutionalDeliveryProofStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.infrastructure.InstitutionalTimelineStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryAttempt;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Usuario;

@Service
public class InstitutionalCommunicationAuditApplicationService {

    private final InstitutionalTimelineStateRepository timelineRepository;
    private final InstitutionalDeliveryProofStateRepository proofRepository;
    private final AuditLedgerService auditLedgerService;

    public InstitutionalCommunicationAuditApplicationService(InstitutionalTimelineStateRepository timelineRepository,
                                                             InstitutionalDeliveryProofStateRepository proofRepository,
                                                             AuditLedgerService auditLedgerService) {
        this.timelineRepository = Objects.requireNonNull(timelineRepository);
        this.proofRepository = Objects.requireNonNull(proofRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    public void registrarDisponibilizacao(InstitutionalInboxItem item, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("detalhe", detalhe);
        metadata.put("canalPrincipal", item.canalPrincipal());
        persist(item, null, InstitutionalTimelineEventType.COMUNICACAO_DISPONIBILIZADA, "Comunicação institucional disponibilizada na caixa.", metadata, "DISPONIBILIZACAO_CAIXA", detalhe);
    }

    public void registrarRecebimento(InstitutionalInboxItem item, Usuario actor, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.COMUNICACAO_RECEBIDA, "Comunicação institucional recebida pela caixa atual.", metadata, "RECEBIMENTO_CAIXA", detalhe);
    }

    public void registrarRedistribuicao(InstitutionalInboxItem item, Usuario actor, String caixaOrigem, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("caixaOrigem", caixaOrigem);
        metadata.put("caixaDestino", item.caixaCodigoAtual());
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.COMUNICACAO_REDISTRIBUIDA, "Comunicação institucional redistribuída dentro da unidade.", metadata, "REDISTRIBUICAO_INTERNA", detalhe);
    }

    public void registrarCiencia(InstitutionalInboxItem item, Usuario actor, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.COMUNICACAO_CIENTIFICADA, "Ciência institucional registrada.", metadata, "CIENCIA_INSTITUCIONAL", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.CERTIDAO_GERADA, "Certidão sistêmica de ciência emitida.", metadata, "CERTIDAO_CIENCIA", detalhe);
    }

    public void registrarCumprimento(InstitutionalInboxItem item, Usuario actor, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.COMUNICACAO_CUMPRIDA, "Fluxo institucional marcado como cumprido.", metadata, "CUMPRIMENTO_INSTITUCIONAL", detalhe);
    }

    public void registrarEntregaEnfileirada(InstitutionalInboxItem item, InstitutionalDeliveryJob job) {
        Map<String, Object> metadata = metadataFromJob(job);
        persist(item, null, InstitutionalTimelineEventType.ENTREGA_ENFILEIRADA, "Job de entrega institucional enfileirado.", metadata, "ENTREGA_ENFILEIRADA", job.currentChannel().name());
    }

    public void registrarTentativaEntrega(InstitutionalInboxItem item, InstitutionalDeliveryJob job, InstitutionalDeliveryAttempt attempt) {
        Map<String, Object> metadata = metadataFromJob(job);
        metadata.put("attemptNumber", attempt.attemptNumber());
        metadata.put("providerStatus", attempt.providerStatus());
        metadata.put("attemptStatus", attempt.status().name());
        persist(item, null, InstitutionalTimelineEventType.ENTREGA_TENTADA, "Tentativa de entrega institucional executada.", metadata, "ENTREGA_TENTADA", attempt.detail());
    }

    public void registrarEntregaEncaminhada(InstitutionalInboxItem item, InstitutionalDeliveryJob job, String detalhe) {
        Map<String, Object> metadata = metadataFromJob(job);
        metadata.put("detalhe", detalhe);
        persist(item, null, InstitutionalTimelineEventType.ENTREGA_ENCAMINHADA, "Entrega institucional encaminhada ao canal externo.", metadata, "ENTREGA_ENCAMINHADA", detalhe);
    }

    public void registrarEntregaConfirmada(InstitutionalInboxItem item, InstitutionalDeliveryJob job, String detalhe) {
        Map<String, Object> metadata = metadataFromJob(job);
        metadata.put("detalhe", detalhe);
        persist(item, null, InstitutionalTimelineEventType.ENTREGA_CONFIRMADA, "Entrega institucional confirmada.", metadata, "ENTREGA_CONFIRMADA", detalhe);
    }

    public void registrarEntregaRetryAgendado(InstitutionalInboxItem item, InstitutionalDeliveryJob job, String detalhe) {
        Map<String, Object> metadata = metadataFromJob(job);
        metadata.put("detalhe", detalhe);
        metadata.put("nextAttemptAt", job.nextAttemptAt().toString());
        persist(item, null, InstitutionalTimelineEventType.ENTREGA_RETRY_AGENDADO, "Retentativa de entrega institucional agendada.", metadata, "ENTREGA_RETRY", detalhe);
    }

    public void registrarEntregaFalhaTerminal(InstitutionalInboxItem item, InstitutionalDeliveryJob job, String detalhe) {
        Map<String, Object> metadata = metadataFromJob(job);
        metadata.put("detalhe", detalhe);
        metadata.put("failureReason", job.lastFailureReason() == null ? null : job.lastFailureReason().name());
        persist(item, null, InstitutionalTimelineEventType.ENTREGA_FALHA_TERMINAL, "Entrega institucional alcançou falha terminal.", metadata, "ENTREGA_FALHA_TERMINAL", detalhe);
    }

    public void registrarEntregaMovidaDlq(InstitutionalInboxItem item, InstitutionalDeliveryJob job, String detalhe) {
        Map<String, Object> metadata = metadataFromJob(job);
        metadata.put("detalhe", detalhe);
        persist(item, null, InstitutionalTimelineEventType.ENTREGA_MOVIDA_DLQ, "Entrega institucional movida para dead-letter queue.", metadata, "ENTREGA_DLQ", detalhe);
    }

    public void registrarGateCriado(InstitutionalInboxItem item, InstitutionalGateState gate) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("gateCode", gate.gateCode());
        metadata.put("statusGate", gate.status().name());
        persist(item, null, InstitutionalTimelineEventType.GATE_CRIADO, "Gate institucional criado para bloqueio do fluxo processual.", metadata, "GATE_CRIADO", gate.gateCode());
    }

    public void registrarGateTransicao(InstitutionalInboxItem item, Usuario actor, InstitutionalGateState gate, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("gateCode", gate.gateCode());
        metadata.put("statusGate", gate.status().name());
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.GATE_TRANSICIONADO, "Gate institucional alterou estado interno.", metadata, "GATE_TRANSICAO", detalhe);
    }

    public void registrarGateLiberado(InstitutionalInboxItem item, Usuario actor, InstitutionalGateState gate, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("gateCode", gate.gateCode());
        metadata.put("statusGate", gate.status().name());
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.GATE_LIBERADO, "Gate institucional liberado.", metadata, "GATE_LIBERADO", detalhe);
    }

    public void registrarDelegacao(InstitutionalInboxItem item, Usuario actor, Long delegadoUsuarioId, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("delegadoUsuarioId", delegadoUsuarioId);
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.DELEGACAO_REGISTRADA, "Delegação institucional registrada para tratamento interno.", metadata, "DELEGACAO_INSTITUCIONAL", detalhe);
    }

    public void registrarSubstituicao(InstitutionalInboxItem item, Usuario actor, Long substitutoUsuarioId, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("substitutoUsuarioId", substitutoUsuarioId);
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.SUBSTITUICAO_REGISTRADA, "Substituição institucional registrada para continuidade operacional.", metadata, "SUBSTITUICAO_INSTITUCIONAL", detalhe);
    }

    public void registrarMinutaCriada(InstitutionalInboxItem item, Usuario actor, String draftId, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("draftId", draftId);
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.MINUTA_CRIADA, "Minuta institucional criada.", metadata, "MINUTA_CRIADA", detalhe);
    }

    public void registrarMinutaSubmetida(InstitutionalInboxItem item, Usuario actor, String draftId, Long aprovadorUsuarioId, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("draftId", draftId);
        metadata.put("aprovadorUsuarioId", aprovadorUsuarioId);
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.MINUTA_ENCAMINHADA_APROVACAO, "Minuta institucional encaminhada para aprovação.", metadata, "MINUTA_SUBMETIDA", detalhe);
    }

    public void registrarMinutaAprovada(InstitutionalInboxItem item, Usuario actor, String draftId, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("draftId", draftId);
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.MINUTA_APROVADA, "Minuta institucional aprovada.", metadata, "MINUTA_APROVADA", detalhe);
    }

    public void registrarMinutaRejeitada(InstitutionalInboxItem item, Usuario actor, String draftId, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("draftId", draftId);
        metadata.put("detalhe", detalhe);
        persist(item, actor, InstitutionalTimelineEventType.MINUTA_REJEITADA, "Minuta institucional rejeitada.", metadata, "MINUTA_REJEITADA", detalhe);
    }


    public void registrarCertidaoNaoLeitura(InstitutionalInboxItem item, Usuario actor, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("detalhe", detalhe);
        metadata.put("prazoCienciaEm", item.prazoCienciaEm().toString());
        persist(item, actor, InstitutionalTimelineEventType.CERTIDAO_NAO_LEITURA_GERADA, "Certidão sistêmica de não leitura/decurso gerada.", metadata, "CERTIDAO_NAO_LEITURA", detalhe);
    }

    public void registrarAvisoExterno(InstitutionalInboxItem item, String canalAviso, String detalhe) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("canalAviso", canalAviso);
        metadata.put("detalhe", detalhe);
        persist(item, null, InstitutionalTimelineEventType.AVISO_EXTERNO_ENFILEIRADO, "Aviso externo acessório enfileirado.", metadata, "AVISO_EXTERNO", detalhe);
    }

    public List<InstitutionalTimelineEvent> timeline(String expedicaoUuid) {
        return timelineRepository.findByExpedicaoUuid(expedicaoUuid);
    }

    public List<InstitutionalDeliveryProof> provas(String expedicaoUuid) {
        return proofRepository.findByExpedicaoUuid(expedicaoUuid);
    }

    private Map<String, Object> metadataFromJob(InstitutionalDeliveryJob job) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", job.jobId());
        metadata.put("channel", job.currentChannel().name());
        metadata.put("attemptCount", job.attemptCount());
        metadata.put("statusEntrega", job.status().name());
        metadata.put("providerReference", job.providerReference());
        return metadata;
    }

    private void persist(InstitutionalInboxItem item,
                         Usuario actor,
                         InstitutionalTimelineEventType eventType,
                         String resumo,
                         Map<String, Object> detalhes,
                         String etapaProva,
                         String evidencia) {
        Instant now = Instant.now();
        String eventId = UUID.nameUUIDFromBytes((item.expedicaoUuid() + "|" + eventType.name() + "|" + now.toString()).getBytes(StandardCharsets.UTF_8)).toString();
        InstitutionalTimelineEvent event = new InstitutionalTimelineEvent(
                eventId,
                item.expedicaoUuid(),
                item.processoId(),
                item.processoNumero(),
                eventType,
                item.status(),
                item.unidadeCodigo(),
                item.caixaCodigoAtual(),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getTipoUsuario() : null,
                resumo,
                detalhes,
                now,
                Hashes.sha256Hex(item.expedicaoUuid() + "|" + eventType.name() + "|" + item.status().name() + "|" + now.toString())
        );
        timelineRepository.save(event);
        InstitutionalDeliveryProof proof = new InstitutionalDeliveryProof(
                UUID.nameUUIDFromBytes((item.expedicaoUuid() + "|" + etapaProva + "|" + now.toString()).getBytes(StandardCharsets.UTF_8)).toString(),
                item.expedicaoUuid(),
                item.processoId(),
                etapaProva,
                item.canalPrincipal(),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getTipoUsuario() : null,
                etapaProva,
                evidencia == null || evidencia.isBlank() ? resumo : evidencia,
                now,
                Hashes.sha256Hex(item.expedicaoUuid() + "|" + etapaProva + "|" + now.toString())
        );
        proofRepository.save(proof);
        auditLedgerService.appendSafely("INSTITUTIONAL_" + eventType.name(), "EXPEDICAO_JUDICIAL", item.expedicaoUuid(), event.hashIntegridade(), resumo);
    }
}
