package com.tcc.pjb.backend.core.processo.unificado.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaActorContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaSigiloContexto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaOperacaoInstitucionalAggregate;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalInboxItemSnapshot;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalInboxItemSnapshotRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaOperacaoInstitucionalApplicationService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalInboxItemSnapshotRepository inboxSnapshotRepository;
    private final ProcessoMalhaExecucaoAssistidaApplicationService processoMalhaExecucaoAssistidaApplicationService;
    private final ObjectMapper objectMapper;
    private final OutboxPublisher outboxPublisher;
    private final AuditLedgerService auditLedgerService;

    public ProcessoMalhaOperacaoInstitucionalApplicationService(ProcessoRepository processoRepository,
                                                                WorkItemRepository workItemRepository,
                                                                InstitutionalInboxItemSnapshotRepository inboxSnapshotRepository,
                                                                ProcessoMalhaExecucaoAssistidaApplicationService processoMalhaExecucaoAssistidaApplicationService,
                                                                ObjectMapper objectMapper,
                                                                OutboxPublisher outboxPublisher,
                                                                AuditLedgerService auditLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.inboxSnapshotRepository = Objects.requireNonNull(inboxSnapshotRepository);
        this.processoMalhaExecucaoAssistidaApplicationService = Objects.requireNonNull(processoMalhaExecucaoAssistidaApplicationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public ProcessoMalhaOperacaoInstitucionalAggregate materializar(Long processoId,
                                                                    ProcessoMalhaActorContext actor,
                                                                    ProcessoMalhaSigiloContexto sigiloContexto) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        var execucao = processoMalhaExecucaoAssistidaApplicationService.executar(processoId);
        String templateCode = "PJB_MALHA_NACIONAL:" + processoId;
        String queueCode = queueCode(actor, execucao.statusExecucao(), sigiloContexto);
        String inboxKey = "MALHA:" + actor.papelEfetivo().name() + ":" + (processo.getTribunal() == null ? "NACIONAL" : processo.getTribunal());
        WorkItem workItem = workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, templateCode).orElseGet(() -> new WorkItem());
        workItem.setProcesso(processo);
        workItem.setFaseOrigem(FaseProcessual.CONHECIMENTO);
        workItem.setTemplateCode(templateCode);
        workItem.setType(WorkItemType.DISTRIBUICAO);
        workItem.setTitulo("Malha nacional institucional");
        workItem.setDescricao("Status=" + execucao.statusExecucao() + "; ação=" + execucao.acaoRecomendada() + "; sigilo=" + sigiloContexto.viewLevel().name());
        workItem.setQueueCode(queueCode);
        workItem.setInboxKey(inboxKey);
        workItem.setAssignedRole(actor.papelEfetivo());
        workItem.setStatus(WorkItemStatus.PENDENTE);
        workItem.setPrioridade(execucao.fechamento().distribuicao().bloqueada() ? 9 : sigiloContexto.acessoSensivel() ? 7 : 5);
        workItem.setBlocking(execucao.fechamento().distribuicao().bloqueada());
        workItem.setDueAt(Instant.now().plus(execucao.fechamento().distribuicao().bloqueada() ? 6 : 24, ChronoUnit.HOURS));
        workItem.setUf(processo.getUf());
        workItem.setComarca(processo.getComarca());
        workItem.setBaseLegal(String.join(" | ", execucao.fundamentos().stream().limit(8).toList()));
        WorkItem persistedWorkItem = workItemRepository.save(workItem);

        String expedicaoUuid = DeterministicUuid.v5("PJB_MALHA_INBOX", processoId + ":" + actor.papelEfetivo().name()).toString();
        String snapshotJson = snapshotJson(processo, actor, sigiloContexto, execucao, persistedWorkItem, queueCode, inboxKey);
        String snapshotHash = Hashes.sha256Hex(snapshotJson);
        InstitutionalInboxItemSnapshot snapshot = inboxSnapshotRepository.findByExpedicaoUuid(expedicaoUuid)
                .map(existing -> refresh(existing, queueCode, snapshotHash, snapshotJson))
                .orElseGet(() -> create(expedicaoUuid, processoId, queueCode, inboxKey, snapshotHash, snapshotJson));

        auditLedgerService.appendSafely(
                "MALHA_OPERACAO_MATERIALIZADA",
                "PROCESSO",
                String.valueOf(processoId),
                snapshotHash,
                "workItem=" + persistedWorkItem.getId() + ";inboxSnapshot=" + snapshot.getId()
        );
        outboxPublisher.enqueue(
                "processo.malha.operacao",
                "MALHA_OPERACAO_MATERIALIZADA",
                Map.of(
                        "processoId", processoId,
                        "workItemId", persistedWorkItem.getId(),
                        "inboxSnapshotId", snapshot.getId(),
                        "queueCode", queueCode,
                        "inboxKey", inboxKey,
                        "snapshotHash", snapshotHash
                ),
                Map.of(
                        "papel", actor.papelEfetivo().name(),
                        "sigilo", sigiloContexto.viewLevel().name()
                ),
                "malha-operacao:" + processoId + ":" + actor.papelEfetivo().name(),
                "PROCESSO_MALHA_OPERACAO",
                String.valueOf(processoId)
        );

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(execucao.fundamentos());
        fundamentos.addAll(sigiloContexto.fundamentos());
        fundamentos.add("operacao.queue=" + queueCode);
        fundamentos.add("operacao.inbox=" + inboxKey);
        fundamentos.add("operacao.workitem=" + persistedWorkItem.getId());
        fundamentos.add("operacao.snapshot=" + snapshot.getId());
        return new ProcessoMalhaOperacaoInstitucionalAggregate(
                processoId,
                processo.getNumero(),
                persistedWorkItem.getId(),
                snapshot.getId(),
                queueCode,
                inboxKey,
                "MATERIALIZADA",
                snapshotHash,
                List.copyOf(fundamentos.stream().limit(120).toList()),
                Instant.now()
        );
    }

    private InstitutionalInboxItemSnapshot refresh(InstitutionalInboxItemSnapshot existing,
                                                   String queueCode,
                                                   String snapshotHash,
                                                   String snapshotJson) {
        existing.refresh(queueCode, "ATIVO", Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), snapshotHash, snapshotJson, Instant.now());
        return inboxSnapshotRepository.save(existing);
    }

    private InstitutionalInboxItemSnapshot create(String expedicaoUuid,
                                                  Long processoId,
                                                  String queueCode,
                                                  String inboxKey,
                                                  String snapshotHash,
                                                  String snapshotJson) {
        InstitutionalInboxItemSnapshot snapshot = new InstitutionalInboxItemSnapshot(
                inboxKey + ":" + processoId,
                expedicaoUuid,
                processoId,
                queueCode,
                inboxKey,
                "ATIVO",
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.DAYS),
                snapshotHash,
                snapshotJson,
                Instant.now(),
                Instant.now()
        );
        return inboxSnapshotRepository.save(snapshot);
    }

    private String snapshotJson(Processo processo,
                                ProcessoMalhaActorContext actor,
                                ProcessoMalhaSigiloContexto sigilo,
                                Object execucao,
                                WorkItem workItem,
                                String queueCode,
                                String inboxKey) {
        try {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("processoId", processo.getId());
            payload.put("numeroProcesso", processo.getNumero());
            payload.put("papelEfetivo", actor.papelEfetivo().name());
            payload.put("ramoEfetivo", actor.ramoEfetivo() == null ? "NAO_INFORMADO" : actor.ramoEfetivo().name());
            payload.put("sigilo", sigilo.viewLevel().name());
            payload.put("queueCode", queueCode);
            payload.put("inboxKey", inboxKey);
            payload.put("workItemId", workItem.getId());
            payload.put("execucao", execucao);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar snapshot institucional da malha", e);
        }
    }

    private String queueCode(ProcessoMalhaActorContext actor,
                             String statusExecucao,
                             ProcessoMalhaSigiloContexto sigiloContexto) {
        String prefixo = statusExecucao == null ? "MALHA" : statusExecucao.trim();
        String sigilo = sigiloContexto.viewLevel().name();
        return prefixo + ":" + actor.papelEfetivo().name() + ":" + sigilo;
    }
}
