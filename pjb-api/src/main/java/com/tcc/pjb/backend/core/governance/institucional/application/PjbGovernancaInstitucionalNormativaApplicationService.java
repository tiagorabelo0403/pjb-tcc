package com.tcc.pjb.backend.core.governance.institucional.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.governance.institucional.domain.PjbGovernancaInstitucionalMarco;
import com.tcc.pjb.backend.core.governance.institucional.domain.PjbGovernancaInstitucionalNormativaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbGovernancaInstitucionalNormativaApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;

    public PjbGovernancaInstitucionalNormativaApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                                 ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                                 ProcessoMalhaSupportBridge processoMalhaSupportBridge) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
    }

    @Transactional(readOnly = true)
    public PjbGovernancaInstitucionalNormativaAggregate avaliar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoRuntimePreparationAggregate runtime = processoRuntimePreparationApplicationService.avaliar(contexto);
        PjbGovernancaInstitucionalMarco tribunais = marco("governanca.tribunais", "Adoção por tribunais", runtime.integrationStatus().competenciaNacionalDisponivel(), runtime.integrationStatus().competenciaNacionalDisponivel() ? 74 : 46, "comite-de-implantacao-por-tribunal", "formalizar tribunal piloto e onda de adesao", List.of("competenciaNacional=" + runtime.integrationStatus().competenciaNacionalDisponivel(), "tribunal=" + contexto.tribunal()));
        PjbGovernancaInstitucionalMarco cnj = marco("governanca.cnj", "Compatibilidade CNJ e governança nacional", runtime.integrationStatus().observabilidadeNacionalDisponivel(), runtime.integrationStatus().observabilidadeNacionalDisponivel() ? 72 : 44, "compatibilidade-pje-jusbr-governanca", "submeter matriz de aderencia normativa", List.of("observabilidadeNacional=" + runtime.integrationStatus().observabilidadeNacionalDisponivel()));
        PjbGovernancaInstitucionalMarco comites = marco("governanca.comites", "Comitês gestores e rito decisório", runtime.integrationStatus().auditLedgerDisponivel() && runtime.integrationStatus().decisionTraceDisponivel(), runtime.integrationStatus().auditLedgerDisponivel() ? 78 : 42, "comite-gestor-executivo", "instituir trilha decisoria auditavel", List.of("audit=" + runtime.integrationStatus().auditLedgerDisponivel(), "trace=" + runtime.integrationStatus().decisionTraceDisponivel()));
        PjbGovernancaInstitucionalMarco politica = marco("governanca.implantacao", "Política de implantação e rollout", runtime.integrationStatus().outboxDisponivel(), runtime.integrationStatus().outboxDisponivel() ? 76 : 40, "rollout-por-onda", "formalizar politica de sombra, corte e reversao", List.of("outbox=" + runtime.integrationStatus().outboxDisponivel()));
        List<PjbGovernancaInstitucionalMarco> marcos = List.of(tribunais, cnj, comites, politica);
        int scoreGeral = (int) Math.round(marcos.stream().mapToInt(PjbGovernancaInstitucionalMarco::score).average().orElse(0));
        List<String> pendencias = marcos.stream().filter(item -> item.status() != PjbFechamentoStatus.CONCLUIDA).map(PjbGovernancaInstitucionalMarco::codigo).toList();
        PjbFechamentoStatus statusGeral = pendencias.isEmpty() && scoreGeral >= 85 ? PjbFechamentoStatus.CONCLUIDA : scoreGeral >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("tribunal=" + contexto.tribunal());
        fundamentos.add("uf=" + contexto.uf());
        fundamentos.add("ramo=" + (contexto.ramoDireito() == null ? "NAO_INFORMADO" : contexto.ramoDireito().name()));
        fundamentos.addAll(pendencias);
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("plataforma.governanca.institucional", "Processo", String.valueOf(processoId), BigDecimal.valueOf(scoreGeral), marcos.toString(), pendencias.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(marcos.toString()), "PJB-GOV", statusGeral.name());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("PJB_GOVERNANCA_AVALIADA", "Processo", String.valueOf(processoId), Hashes.sha256Hex(marcos.toString()), "status=" + statusGeral.name() + ";score=" + scoreGeral);
        }
        return new PjbGovernancaInstitucionalNormativaAggregate(
                processoId,
                contexto.numeroReferencia(),
                marcos,
                scoreGeral,
                statusGeral,
                statusGeral == PjbFechamentoStatus.CONCLUIDA,
                pendencias,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private PjbGovernancaInstitucionalMarco marco(String codigo,
                                                  String titulo,
                                                  boolean concluido,
                                                  int score,
                                                  String trilhaInstitucional,
                                                  String proximaDeliberacao,
                                                  List<String> fundamentos) {
        PjbFechamentoStatus status = concluido ? PjbFechamentoStatus.CONCLUIDA : score >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.PENDENTE;
        return new PjbGovernancaInstitucionalMarco(codigo, titulo, status, score, trilhaInstitucional, proximaDeliberacao, fundamentos);
    }
}
