package com.tcc.pjb.backend.core.quality.certificacao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalItem;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbCertificacaoOperacionalApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;
    private final PjbCodebaseSanityApplicationService pjbCodebaseSanityApplicationService;
    private final PjbApiSurfaceSanityApplicationService pjbApiSurfaceSanityApplicationService;

    public PjbCertificacaoOperacionalApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                        ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                        ProcessoMalhaSupportBridge processoMalhaSupportBridge,
                                                        PjbCodebaseSanityApplicationService pjbCodebaseSanityApplicationService,
                                                        PjbApiSurfaceSanityApplicationService pjbApiSurfaceSanityApplicationService) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
        this.pjbCodebaseSanityApplicationService = Objects.requireNonNull(pjbCodebaseSanityApplicationService);
        this.pjbApiSurfaceSanityApplicationService = Objects.requireNonNull(pjbApiSurfaceSanityApplicationService);
    }

    @Transactional(readOnly = true)
    public PjbCertificacaoOperacionalAggregate certificar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoRuntimePreparationAggregate runtime = processoRuntimePreparationApplicationService.avaliar(contexto);
        PjbCodebaseSanityAggregate codebase = pjbCodebaseSanityApplicationService.auditar();
        PjbApiSurfaceSanityAggregate apiSurface = pjbApiSurfaceSanityApplicationService.auditar();
        List<PjbCertificacaoOperacionalItem> itens = new ArrayList<>();
        itens.add(item("runtime.prontidao", "runtime", runtime.integrationStatus().prontoMinimo(), runtime.integrationStatus().percentualProntidao() >= 75 ? "INFO" : "CRITICO", "Prontidão calculada em " + runtime.integrationStatus().percentualProntidao() + "%", "Conectar componentes ausentes do runtime"));
        itens.add(item("runtime.alertas", "runtime", runtime.alertas().isEmpty(), runtime.alertas().isEmpty() ? "INFO" : "ALTO", runtime.alertas().toString(), "Eliminar alertas estruturais e regressões"));
        itens.add(item("malha.documental", "documental", runtime.integrationStatus().documentoProcessualDisponivel(), runtime.integrationStatus().documentoProcessualDisponivel() ? "INFO" : "ALTO", "Disponibilidade documental=" + runtime.integrationStatus().documentoProcessualDisponivel(), "Garantir repositório documental"));
        itens.add(item("malha.auditabilidade", "governanca", runtime.integrationStatus().auditLedgerDisponivel() && runtime.integrationStatus().decisionTraceDisponivel(), runtime.integrationStatus().auditLedgerDisponivel() && runtime.integrationStatus().decisionTraceDisponivel() ? "INFO" : "CRITICO", "Audit e explainability precisam estar ativos", "Reativar trilha auditável"));
        itens.add(item("codebase.integridade", "codebase", codebase.disponivel() && codebase.limpo(), codebase.disponivel() && codebase.limpo() ? "INFO" : codebase.disponivel() ? "ALTO" : "MODERADO", "Codebase=" + codebase.resumo() + ", score=" + codebase.score(), "Eliminar imports quebrados, duplicidades e virtual threads diretas"));
        itens.add(item("codebase.imports", "codebase", codebase.importsInternosQuebrados() == 0, codebase.importsInternosQuebrados() == 0 ? "INFO" : "CRITICO", "Imports internos quebrados=" + codebase.importsInternosQuebrados(), "Reconciliar contratos e classes antigas"));
        itens.add(item("codebase.virtualthreads", "performance", codebase.virtualThreadsDiretas() == 0, codebase.virtualThreadsDiretas() == 0 ? "INFO" : "ALTO", "Virtual threads diretas=" + codebase.virtualThreadsDiretas(), "Migrar uso direto para lanes bounded do projeto"));
        itens.add(item("codebase.repositories", "codebase", codebase.issues().stream().noneMatch(issue -> "repository.entity.quebrada".equals(issue.codigo())), codebase.issues().stream().noneMatch(issue -> "repository.entity.quebrada".equals(issue.codigo())) ? "INFO" : "CRITICO", "Repositorios com entidade quebrada=" + codebase.issues().stream().filter(issue -> "repository.entity.quebrada".equals(issue.codigo())).count(), "Reconciliar generics de repositorio com entidades reais"));
        itens.add(item("api.surface", "api", apiSurface.limpo(), apiSurface.limpo() ? "INFO" : apiSurface.rotasDuplicadas() > 0 ? "CRITICO" : "ALTO", "API score=" + apiSurface.score() + ", rotasDuplicadas=" + apiSurface.rotasDuplicadas() + ", dtoForaDoPadrao=" + apiSurface.dtoForaDoPadrao(), "Reconciliar controllers, DTOs e contratos HTTP legados"));
        int cobertura = itens.isEmpty() ? 0 : (int) Math.round(itens.stream().filter(PjbCertificacaoOperacionalItem::conforme).count() * 100.0d / itens.size());
        List<String> modulosCriticos = itens.stream().filter(item -> !item.conforme()).map(PjbCertificacaoOperacionalItem::codigo).toList();
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("quality.certificacao.operacional", "Processo", String.valueOf(processoId), BigDecimal.valueOf(cobertura), itens.toString(), itens.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(itens.toString()), "PJB-CERT", modulosCriticos.toString());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("CERTIFICACAO_OPERACIONAL_GERADA", "Processo", String.valueOf(processoId), Hashes.sha256Hex(itens.toString()), "cobertura=" + cobertura);
        }
        return new PjbCertificacaoOperacionalAggregate(
                processoId,
                contexto.numeroReferencia(),
                List.copyOf(itens),
                cobertura,
                itens.stream().anyMatch(item -> !item.conforme() && "CRITICO".equals(item.severidade())),
                modulosCriticos,
                Instant.now()
        );
    }

    private PjbCertificacaoOperacionalItem item(String codigo,
                                                String categoria,
                                                boolean conforme,
                                                String severidade,
                                                String diagnostico,
                                                String acaoCorretiva) {
        return new PjbCertificacaoOperacionalItem(codigo, categoria, severidade, conforme, diagnostico, acaoCorretiva);
    }
}
