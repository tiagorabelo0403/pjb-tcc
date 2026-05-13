package com.tcc.pjb.backend.core.processo.migracao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoFabricaAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoFabricaItem;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalInboxItemSnapshotRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMigracaoFactoryApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;
    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalInboxItemSnapshotRepository institutionalInboxItemSnapshotRepository;

    public ProcessoMigracaoFactoryApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                     ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                     ProcessoMalhaSupportBridge processoMalhaSupportBridge,
                                                     ProcessoRepository processoRepository,
                                                     ObjectProvider<DocumentoProcessualRepository> documentoProcessualRepositoryProvider,
                                                     ObjectProvider<WorkItemRepository> workItemRepositoryProvider,
                                                     ObjectProvider<InstitutionalInboxItemSnapshotRepository> institutionalInboxItemSnapshotRepositoryProvider) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = documentoProcessualRepositoryProvider.getIfAvailable();
        this.workItemRepository = workItemRepositoryProvider.getIfAvailable();
        this.institutionalInboxItemSnapshotRepository = institutionalInboxItemSnapshotRepositoryProvider.getIfAvailable();
    }

    @Transactional(readOnly = true)
    public ProcessoMigracaoFabricaAggregate planejar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoRuntimePreparationAggregate runtime = processoRuntimePreparationApplicationService.avaliar(contexto);
        List<ProcessoMigracaoFabricaItem> itens = new ArrayList<>();
        itens.add(item("migracao.acervo", "Acervo processual", true, contexto.numeroReferencia().isBlank() ? PjbFechamentoStatus.PENDENTE : PjbFechamentoStatus.CONCLUIDA, contexto.numeroReferencia().isBlank() ? 35 : 88, "legacy:acervo", "pjb:processo", "bulk+checksum", List.of("numero=" + contexto.numeroReferencia())));
        itens.add(item("migracao.classes", "Classes e assuntos", true, contexto.processo().getClasseProcessual() == null || contexto.processo().getClasseProcessual().isBlank() ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.CONCLUIDA, contexto.processo().getClasseProcessual() == null || contexto.processo().getClasseProcessual().isBlank() ? 62 : 90, "legacy:classe", "pjb:classe-processual", "catalogo+alias", List.of("classe=" + Objects.toString(contexto.processo().getClasseProcessual(), ""))));
        itens.add(item("migracao.movimentos", "Movimentos processuais", true, runtime.integrationStatus().observabilidadeProcessualDisponivel() ? PjbFechamentoStatus.CONCLUIDA : PjbFechamentoStatus.PARCIAL, runtime.integrationStatus().observabilidadeProcessualDisponivel() ? 84 : 58, "legacy:movimento", "pjb:timeline", "sequencial+dedupe", List.of("observabilidadeProcessual=" + runtime.integrationStatus().observabilidadeProcessualDisponivel())));
        itens.add(item("migracao.documentos", "Documentos e anexos", true, documentoProcessualRepository != null ? PjbFechamentoStatus.CONCLUIDA : PjbFechamentoStatus.PENDENTE, documentoProcessualRepository != null ? 86 : 30, "legacy:documento", "pjb:documento-processual", "hash+lacre", List.of("repoDocumental=" + (documentoProcessualRepository != null))));
        itens.add(item("migracao.assinaturas", "Assinaturas e rastreabilidade", true, runtime.integrationStatus().auditLedgerDisponivel() ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.PENDENTE, runtime.integrationStatus().auditLedgerDisponivel() ? 68 : 28, "legacy:assinatura", "pjb:audit-ledger", "revalidacao+evidencia", List.of("audit=" + runtime.integrationStatus().auditLedgerDisponivel())));
        itens.add(item("migracao.perfis", "Perfis e atores", true, runtime.integrationStatus().usuarioRepositoryDisponivel() ? PjbFechamentoStatus.CONCLUIDA : PjbFechamentoStatus.PENDENTE, runtime.integrationStatus().usuarioRepositoryDisponivel() ? 82 : 25, "legacy:perfil", "pjb:identidade-ator", "mapping+governanca", List.of("usuarioRepository=" + runtime.integrationStatus().usuarioRepositoryDisponivel())));
        itens.add(item("migracao.filas", "Filas e caixas", true, workItemRepository != null ? PjbFechamentoStatus.CONCLUIDA : PjbFechamentoStatus.PARCIAL, workItemRepository != null ? 83 : 55, "legacy:fila", "pjb:workitem", "wave+reprocessamento", List.of("workItemRepository=" + (workItemRepository != null))));
        itens.add(item("migracao.sigilos", "Sigilos e restrições", true, contexto.sigiloReforcado() ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.CONCLUIDA, contexto.sigiloReforcado() ? 66 : 86, "legacy:sigilo", "pjb:sigilo-contextual", "step-up+mascaramento", List.of("sigiloReforcado=" + contexto.sigiloReforcado())));
        itens.add(item("migracao.integracoes", "Integrações legadas", true, institutionalInboxItemSnapshotRepository != null && runtime.integrationStatus().outboxDisponivel() ? PjbFechamentoStatus.CONCLUIDA : PjbFechamentoStatus.PARCIAL, institutionalInboxItemSnapshotRepository != null && runtime.integrationStatus().outboxDisponivel() ? 84 : 57, "legacy:integracao", "pjb:outbox+inbox", "shadow+mirror", List.of("outbox=" + runtime.integrationStatus().outboxDisponivel(), "inboxRepo=" + (institutionalInboxItemSnapshotRepository != null))));
        int scoreGeral = (int) Math.round(itens.stream().mapToInt(ProcessoMigracaoFabricaItem::score).average().orElse(0));
        List<String> bloqueios = itens.stream().filter(item -> item.obrigatorio() && item.status() != PjbFechamentoStatus.CONCLUIDA).map(ProcessoMigracaoFabricaItem::codigo).toList();
        PjbFechamentoStatus statusGeral = bloqueios.isEmpty() && scoreGeral >= 85 ? PjbFechamentoStatus.CONCLUIDA : scoreGeral >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("processos.baseDisponivel=" + processoRepository.existsById(processoId));
        fundamentos.add("runtime.prontidao=" + runtime.integrationStatus().percentualProntidao());
        itens.forEach(item -> fundamentos.add(item.codigo() + "=" + item.status().name()));
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("plataforma.migracao.factory", "Processo", String.valueOf(processoId), BigDecimal.valueOf(scoreGeral), itens.toString(), bloqueios.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(itens.toString()), "PJB-MIG", statusGeral.name());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("PJB_MIGRACAO_FACTORY_PLANEJADA", "Processo", String.valueOf(processoId), Hashes.sha256Hex(itens.toString()), "status=" + statusGeral.name() + ";score=" + scoreGeral);
        }
        return new ProcessoMigracaoFabricaAggregate(
                processoId,
                contexto.numeroReferencia(),
                List.copyOf(itens),
                scoreGeral,
                statusGeral,
                statusGeral == PjbFechamentoStatus.CONCLUIDA,
                bloqueios,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoMigracaoFabricaItem item(String codigo,
                                             String titulo,
                                             boolean obrigatorio,
                                             PjbFechamentoStatus status,
                                             int score,
                                             String origem,
                                             String destino,
                                             String estrategia,
                                             List<String> fundamentos) {
        return new ProcessoMigracaoFabricaItem(codigo, titulo, status, obrigatorio, score, origem, destino, estrategia, fundamentos);
    }
}
