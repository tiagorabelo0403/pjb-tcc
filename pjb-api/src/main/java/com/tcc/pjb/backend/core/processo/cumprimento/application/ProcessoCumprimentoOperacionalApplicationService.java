package com.tcc.pjb.backend.core.processo.cumprimento.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.cumprimento.domain.ProcessoCumprimentoOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.cumprimento.domain.ProcessoCumprimentoOperacionalItem;
import com.tcc.pjb.backend.core.processo.distribuicao.application.ProcessoDistribuicaoMalhaOrquestracaoApplicationService;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaOrquestracaoAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ProcessoCumprimentoOperacionalApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoDistribuicaoMalhaOrquestracaoApplicationService processoDistribuicaoMalhaOrquestracaoApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;
    private final WorkItemRepository workItemRepository;
    private final OutboxPublisher outboxPublisher;

    public ProcessoCumprimentoOperacionalApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                            ProcessoDistribuicaoMalhaOrquestracaoApplicationService processoDistribuicaoMalhaOrquestracaoApplicationService,
                                                            ProcessoMalhaSupportBridge processoMalhaSupportBridge,
                                                            WorkItemRepository workItemRepository,
                                                            OutboxPublisher outboxPublisher) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoDistribuicaoMalhaOrquestracaoApplicationService = Objects.requireNonNull(processoDistribuicaoMalhaOrquestracaoApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
    }

    @PjbTransactionalBudget(operation = "processo.cumprimento-operacional.materializar", maxMillis = 5000)
    @Transactional
    public ProcessoCumprimentoOperacionalAggregate materializar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoDistribuicaoMalhaOrquestracaoAggregate distribuicao = processoDistribuicaoMalhaOrquestracaoApplicationService.executar(processoId);
        List<ProcessoCumprimentoOperacionalItem> planejados = planejar(contexto, distribuicao);
        List<String> codigosPlanejados = planejados.stream().map(ProcessoCumprimentoOperacionalItem::codigo).toList();
        Set<String> codigosJaMaterializados = workItemRepository.findAllByProcesso_IdAndTemplateCodeInAndStatusNot(processoId, codigosPlanejados, WorkItemStatus.CANCELADO).stream()
                .map(WorkItem::getTemplateCode)
                .collect(Collectors.toSet());
        int materializados = 0;
        for (ProcessoCumprimentoOperacionalItem item : planejados) {
            if (!codigosJaMaterializados.contains(item.codigo())) {
                WorkItem workItem = new WorkItem();
                workItem.setProcesso(contexto.processo());
                workItem.setFaseOrigem(resolveFase(contexto));
                workItem.setTemplateCode(item.codigo());
                workItem.setType(item.tipo());
                workItem.setTitulo(item.titulo());
                workItem.setDescricao(item.descricao());
                workItem.setQueueCode(item.queueCode());
                workItem.setInboxKey(item.inboxKey());
                workItem.setAssignedRole(item.papelResponsavel());
                workItem.setPrioridade(item.prioridade());
                workItem.setBlocking(item.bloqueante());
                workItem.setDueAt(item.dueAt());
                workItem.setUf(contexto.uf());
                workItem.setComarca(contexto.comarca());
                workItem.setBaseLegal(item.baseLegal());
                workItemRepository.save(workItem);
                materializados++;
            }
            outboxPublisher.enqueue("cumprimento-operacional", "CUMPRIMENTO_ITEM_PLANEJADO", item, java.util.Map.of("processoId", processoId), item.hashComando(), "Processo", String.valueOf(processoId));
        }
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(distribuicao.fundamentos());
        fundamentos.add("cumprimento.itens=" + planejados.size());
        fundamentos.add("cumprimento.materializados=" + materializados);
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("processo.cumprimento.operacional", "Processo", String.valueOf(processoId), BigDecimal.valueOf(planejados.size() * 10L), fundamentos.toString(), planejados.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(planejados.toString()), "PJB-CUMPRIMENTO", distribuicao.acaoExecutada());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("CUMPRIMENTO_OPERACIONAL_MATERIALIZADO", "Processo", String.valueOf(processoId), Hashes.sha256Hex(planejados.toString()), "itens=" + planejados.size() + ";materializados=" + materializados);
        }
        return new ProcessoCumprimentoOperacionalAggregate(
                processoId,
                contexto.numeroReferencia(),
                planejados,
                materializados,
                planejados.stream().mapToInt(ProcessoCumprimentoOperacionalItem::prioridade).min().orElse(5),
                planejados.stream().anyMatch(ProcessoCumprimentoOperacionalItem::bloqueante),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private List<ProcessoCumprimentoOperacionalItem> planejar(ProcessoRuntimeContext contexto,
                                                              ProcessoDistribuicaoMalhaOrquestracaoAggregate distribuicao) {
        List<ProcessoCumprimentoOperacionalItem> itens = new ArrayList<>();
        itens.add(item(contexto, "cumprimento.snapshot.operacional", "Atualizar snapshot institucional", "Atualiza fotografia operacional do processo e caixas institucionais", WorkItemType.EXPEDICAO, TipoUsuario.SERVIDOR_FORUM, "fila-operacao-institucional", "inbox-operacao-institucional", 2, true, 2, "Poder geral de cautela e governança operacional"));
        if (distribuicao.bloqueada()) {
            itens.add(item(contexto, "cumprimento.triagem.prevento", "Executar triagem por prevento", "Executa remessa ou redistribuição guiada pela malha", WorkItemType.DISTRIBUICAO, TipoUsuario.SERVIDOR_FORUM, "fila-distribuicao-malha", "inbox-distribuicao-malha", 1, true, 1, "Prevenção, conexão e dependência processual"));
        }
        if (contexto.sigiloReforcado()) {
            itens.add(item(contexto, "cumprimento.sigilo.reforco", "Reforçar trilha de sigilo", "Valida acesso sensível, trilha step-up e mascaramento operacional", WorkItemType.DILIGENCIA, TipoUsuario.ASSESSOR_JUDICIAL, "fila-sigilo-reforcado", "inbox-sigilo-reforcado", 1, true, 1, "Segredo de justiça, LGPD e proteção institucional"));
        }
        return List.copyOf(itens);
    }

    private ProcessoCumprimentoOperacionalItem item(ProcessoRuntimeContext contexto,
                                                    String codigo,
                                                    String titulo,
                                                    String descricao,
                                                    WorkItemType tipo,
                                                    TipoUsuario papel,
                                                    String fila,
                                                    String inbox,
                                                    int prioridade,
                                                    boolean bloqueante,
                                                    long dueInDays,
                                                    String baseLegal) {
        UUID uuid = DeterministicUuid.v5("cumprimento-operacional", contexto.processoId() + "#" + codigo);
        String hash = Hashes.sha256Hex(uuid + "#" + codigo + "#" + contexto.numeroReferencia());
        return new ProcessoCumprimentoOperacionalItem(codigo, titulo, descricao, tipo, papel, fila, inbox, prioridade, bloqueante, Instant.now().plus(dueInDays, ChronoUnit.DAYS), baseLegal, hash);
    }

    private FaseProcessual resolveFase(ProcessoRuntimeContext contexto) {
        if (contexto.processo().getFaseAtual() == null) {
            return FaseProcessual.CONHECIMENTO;
        }
        return contexto.processo().getFaseAtual();
    }
}
