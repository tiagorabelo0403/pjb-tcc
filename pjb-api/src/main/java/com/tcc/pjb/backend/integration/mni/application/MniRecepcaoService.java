package com.tcc.pjb.backend.integration.mni.application;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoAtoSummary;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoEnvelope;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniPayloadSnapshot;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoProcessoSnapshot;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoRequest;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRecepcao;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoAuditSnapshot;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoProjection;

@Service
public class MniRecepcaoService {

    private final ProcessoRepository processoRepository;
    private final MniRecepcaoRepository recepcaoRepository;
    private final MniXmlToProcessoAdapter xmlToProcessoAdapter;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;
    private final AuditLedgerService auditLedger;

    public MniRecepcaoService(ProcessoRepository processoRepository,
                              MniRecepcaoRepository recepcaoRepository,
                              MniXmlToProcessoAdapter xmlToProcessoAdapter,
                              ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy,
                              AuditLedgerService auditLedger) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.recepcaoRepository = Objects.requireNonNull(recepcaoRepository);
        this.xmlToProcessoAdapter = Objects.requireNonNull(xmlToProcessoAdapter);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @Transactional
    public MniRecepcaoResult receberAutos(MniRecepcaoCommand command) {
        Objects.requireNonNull(command);
        return receberAutos(command.toRequest());
    }

    @Transactional
    public MniRecepcaoResult receberAutos(MniRecepcaoRequest request) {
        String tribunalOrigem = request == null ? null : request.tribunalOrigem();
        String motivo = request == null ? null : request.motivo();
        String xml = request == null ? null : request.xml();
        String payloadHash = Hashes.sha256Hex(xml == null ? "" : xml);
        MniRecepcao existente = recepcaoRepository.findByMniPayloadHash(payloadHash).orElse(null);
        if (existente != null) {
            return toResult(existente);
        }
        Processo processo = xmlToProcessoAdapter.fromXml(xml, tribunalOrigem, motivo);
        Processo salvo = processoRepository.save(processo);
        MniRecepcao recepcao = MniRecepcao.builder()
                .tribunalOrigem(tribunalOrigem)
                .numeroUnificado(salvo.getNumeroUnificado())
                .processoIdLocal(salvo.getId())
                .motivo(motivo)
                .mniPayloadHash(payloadHash)
                .receivedAt(Instant.now())
                .processedAt(Instant.now())
                .status("PROCESSED")
                .build();
        MniRecepcao persisted = recepcaoRepository.save(recepcao);
        readAfterWriteConsistencyPolicy.markWrite();
        auditLedger.appendSafely("MNI_RECEPCAO_OK", "PROCESSO", String.valueOf(salvo.getId()), "origem=" + tribunalOrigem + " motivo=" + motivo);
        return toResult(persisted);
    }


    @Transactional(readOnly = true)
    public MniRecepcaoEnvelope envelope(Long recepcaoId) {
        MniRecepcao recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        MniRecepcaoAtoSummary ato = new MniRecepcaoAtoSummary(recepcao.getMotivo(), recepcao.getMniPayloadHash());
        MniRecepcaoProcessoSnapshot processo = new MniRecepcaoProcessoSnapshot(recepcao.getProcessoIdLocal(), recepcao.getNumeroUnificado(), "MNI");
        return new MniRecepcaoEnvelope(ato, processo);
    }

    @Transactional(readOnly = true)
    public MniPayloadSnapshot payloadSnapshot(Long recepcaoId) {
        MniRecepcao recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new MniPayloadSnapshot(recepcao.getMniPayloadHash(), recepcao.getNumeroUnificado(), recepcao.getMotivo());
    }

    private MniRecepcaoResult toResult(MniRecepcao recepcao) {
        MniRecepcaoAtoSummary ato = new MniRecepcaoAtoSummary(recepcao.getMotivo(), recepcao.getMniPayloadHash());
        MniRecepcaoProcessoSnapshot processo = new MniRecepcaoProcessoSnapshot(recepcao.getProcessoIdLocal(), recepcao.getNumeroUnificado(), "MNI");
        return new MniRecepcaoResult(recepcao.getTribunalOrigem(), recepcao.getNumeroUnificado(), ato.motivo(), ato.payloadHash(), processo.processoId(), recepcao.getStatus());
    }

    @Transactional(readOnly = true)
    public MniRecepcaoProjection projection(Long recepcaoId) {
        MniRecepcao recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new MniRecepcaoProjection(recepcao.getId(), recepcao.getTribunalOrigem(), recepcao.getMotivo(), recepcao.getStatus(), recepcao.getReceivedAt());
    }

    @Transactional(readOnly = true)
    public MniRecepcaoAuditSnapshot audit(Long recepcaoId) {
        MniRecepcao recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new MniRecepcaoAuditSnapshot(recepcao.getId(), recepcao.getMniPayloadHash(), recepcao.getStatus());
    }


    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniConsultaRecepcaoResult consultar(com.tcc.pjb.backend.integration.mni.domain.MniConsultaRecepcaoCommand command) {
        Objects.requireNonNull(command);
        return new com.tcc.pjb.backend.integration.mni.domain.MniConsultaRecepcaoResult(projection(command.recepcaoId()), audit(command.recepcaoId()));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniEnvelopeSnapshot envelopeSnapshot(Long recepcaoId) {
        var recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniEnvelopeSnapshot(recepcao.getNumeroUnificado(), recepcao.getMotivo(), recepcao.getMniPayloadHash());
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniPayloadAuditSnapshot payloadAudit(Long recepcaoId) {
        var recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniPayloadAuditSnapshot(recepcao.getMniPayloadHash(), recepcao.getNumeroUnificado(), recepcao.getStatus());
    }

    @Transactional(readOnly = true)
    public java.util.List<com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoTimelineEntry> timeline(Long recepcaoId) {
        var recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        java.util.List<com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoTimelineEntry> entries = new java.util.ArrayList<>();
        entries.add(new com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoTimelineEntry("RECEBIDO", recepcao.getReceivedAt(), recepcao.getTribunalOrigem()));
        if (recepcao.getProcessedAt() != null) {
            entries.add(new com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoTimelineEntry("PROCESSADO", recepcao.getProcessedAt(), recepcao.getStatus()));
        }
        return entries;
    }



    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoQueryResult consultar(com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoQuery query) {
        Objects.requireNonNull(query);
        return new com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoQueryResult(
                projection(query.recepcaoId()),
                audit(query.recepcaoId()),
                envelopeSnapshot(query.recepcaoId())
        );
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoStatusSnapshot statusSnapshot(Long recepcaoId) {
        var recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoStatusSnapshot(
                recepcao.getId(),
                recepcao.getStatus(),
                recepcao.getReceivedAt(),
                recepcao.getProcessedAt()
        );
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoFailureResult failureResult(Long recepcaoId) {
        var recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoFailureResult(
                recepcao.getId(),
                recepcao.getStatus(),
                recepcao.getStatus().contains("FAIL") ? recepcao.getStatus() : null
        );
    }



    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoHealthSnapshot healthSnapshot(Long recepcaoId) {
        var recepcao = recepcaoRepository.findById(recepcaoId)
                .orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
        return new com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoHealthSnapshot(recepcao.getId(), recepcao.getStatus(), recepcao.getProcessedAt() != null, recepcao.getMniPayloadHash() != null && !recepcao.getMniPayloadHash().isBlank());
    }



@Transactional(readOnly = true)
public com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoEnvelopeView envelopeView(Long recepcaoId) {
    var recepcao = recepcaoRepository.findById(recepcaoId).orElseThrow(() -> new IllegalArgumentException("MNI recepção não encontrada: " + recepcaoId));
    return new com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoEnvelopeView(recepcao.getNumeroUnificado(), recepcao.getMotivo(), recepcao.getStatus());
}
}
