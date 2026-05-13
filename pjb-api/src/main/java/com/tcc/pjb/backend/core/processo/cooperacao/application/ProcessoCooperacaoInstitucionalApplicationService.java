package com.tcc.pjb.backend.core.processo.cooperacao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.cooperacao.domain.ProcessoCooperacaoInstitucionalAggregate;
import com.tcc.pjb.backend.core.processo.cooperacao.domain.ProcessoCooperacaoInstitucionalItem;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoCooperacaoInstitucionalApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;
    private final OutboxPublisher outboxPublisher;

    public ProcessoCooperacaoInstitucionalApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                             ProcessoMalhaSupportBridge processoMalhaSupportBridge,
                                                             OutboxPublisher outboxPublisher) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
    }

    @Transactional
    public ProcessoCooperacaoInstitucionalAggregate orquestrar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        List<ProcessoCooperacaoInstitucionalItem> itens = new ArrayList<>();
        if (contexto.ramoDireito() != null && contexto.ramoDireito().name().contains("PEN")) {
            itens.add(item(contexto, "cooperacao.policia", "Delegacia/Polícia", "Compartilhamento institucional de diligência e custódia", "48h", "Aguardar retorno de diligência policial"));
        }
        itens.add(item(contexto, "cooperacao.mp", "Ministério Público", "Ciência institucional e eventual manifestação obrigatória", "5d", "Aguardar manifestação ministerial"));
        if (contexto.sigiloReforcado()) {
            itens.add(item(contexto, "cooperacao.sigilo", "Unidade sigilosa competente", "Tramitação reforçada por sigilo contextual e custódia", "24h", "Confirmar canal de cooperação restrito"));
        }
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("cooperacao.itens=" + itens.size());
        fundamentos.add("cooperacao.sigilo=" + contexto.sigiloReforcado());
        for (ProcessoCooperacaoInstitucionalItem item : itens) {
            outboxPublisher.enqueue("cooperacao-institucional", "COOPERACAO_INSTITUCIONAL_SOLICITADA", item, java.util.Map.of("processoId", processoId, "destino", item.destino()), item.chaveCorrelacao(), "Processo", String.valueOf(processoId));
        }
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("processo.cooperacao.institucional", "Processo", String.valueOf(processoId), BigDecimal.valueOf(Math.max(1, itens.size()) * 15L), fundamentos.toString(), itens.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(itens.toString()), "PJB-COOPERACAO", contexto.tribunal());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("COOPERACAO_INSTITUCIONAL_ORQUESTRADA", "Processo", String.valueOf(processoId), Hashes.sha256Hex(itens.toString()), "itens=" + itens.size());
        }
        return new ProcessoCooperacaoInstitucionalAggregate(
                processoId,
                contexto.numeroReferencia(),
                itens,
                !itens.isEmpty(),
                contexto.sigiloReforcado(),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoCooperacaoInstitucionalItem item(ProcessoRuntimeContext contexto,
                                                     String codigo,
                                                     String destino,
                                                     String fundamento,
                                                     String prazo,
                                                     String pendencia) {
        String chave = DeterministicUuid.v5("cooperacao-institucional", contexto.processoId() + "#" + codigo).toString();
        return new ProcessoCooperacaoInstitucionalItem(codigo, destino, fundamento, prazo, pendencia, chave, Instant.now());
    }
}
