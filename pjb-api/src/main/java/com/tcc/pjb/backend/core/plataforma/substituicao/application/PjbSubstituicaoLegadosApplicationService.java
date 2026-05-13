package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.governance.institucional.application.PjbGovernancaInstitucionalNormativaApplicationService;
import com.tcc.pjb.backend.core.governance.institucional.domain.PjbGovernancaInstitucionalNormativaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosProva;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosSistema;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoFabricaAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoProducaoPesadaApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.transicao.application.ProcessoConvivenciaTransicaoApplicationService;
import com.tcc.pjb.backend.core.processo.transicao.domain.ProcessoConvivenciaTransicaoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoBlocosSoberanosRuntimeApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoBlocosSoberanosRuntimeAggregate;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbSubstituicaoLegadosApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;
    private final ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService;
    private final ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService;
    private final ProcessoConvivenciaTransicaoApplicationService processoConvivenciaTransicaoApplicationService;
    private final PjbGovernancaInstitucionalNormativaApplicationService pjbGovernancaInstitucionalNormativaApplicationService;
    private final ProcessoBlocosSoberanosRuntimeApplicationService processoBlocosSoberanosRuntimeApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;

    public PjbSubstituicaoLegadosApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                    ProcessoMalhaParallelExecutor processoMalhaParallelExecutor,
                                                    ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService,
                                                    ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService,
                                                    ProcessoConvivenciaTransicaoApplicationService processoConvivenciaTransicaoApplicationService,
                                                    PjbGovernancaInstitucionalNormativaApplicationService pjbGovernancaInstitucionalNormativaApplicationService,
                                                    ProcessoBlocosSoberanosRuntimeApplicationService processoBlocosSoberanosRuntimeApplicationService,
                                                    ProcessoMalhaSupportBridge processoMalhaSupportBridge) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
        this.processoProducaoPesadaApplicationService = Objects.requireNonNull(processoProducaoPesadaApplicationService);
        this.processoMigracaoFactoryApplicationService = Objects.requireNonNull(processoMigracaoFactoryApplicationService);
        this.processoConvivenciaTransicaoApplicationService = Objects.requireNonNull(processoConvivenciaTransicaoApplicationService);
        this.pjbGovernancaInstitucionalNormativaApplicationService = Objects.requireNonNull(pjbGovernancaInstitucionalNormativaApplicationService);
        this.processoBlocosSoberanosRuntimeApplicationService = Objects.requireNonNull(processoBlocosSoberanosRuntimeApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoLegadosAggregate avaliar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoMalhaParallelExecutor.Quarteto<ProcessoProducaoPesadaAggregate, ProcessoMigracaoFabricaAggregate, ProcessoConvivenciaTransicaoAggregate, PjbGovernancaInstitucionalNormativaAggregate> quarteto = processoMalhaParallelExecutor.executar4(
                "pjb-substituicao-legados",
                (java.util.function.Supplier<ProcessoProducaoPesadaAggregate>) () -> processoProducaoPesadaApplicationService.avaliar(processoId),
                (java.util.function.Supplier<ProcessoMigracaoFabricaAggregate>) () -> processoMigracaoFactoryApplicationService.planejar(processoId),
                (java.util.function.Supplier<ProcessoConvivenciaTransicaoAggregate>) () -> processoConvivenciaTransicaoApplicationService.planejar(processoId),
                (java.util.function.Supplier<PjbGovernancaInstitucionalNormativaAggregate>) () -> pjbGovernancaInstitucionalNormativaApplicationService.avaliar(processoId)
        );
        ProcessoBlocosSoberanosRuntimeAggregate blocos = processoBlocosSoberanosRuntimeApplicationService.avaliar(processoId);
        ProcessoProducaoPesadaAggregate producao = quarteto.primeiro();
        ProcessoMigracaoFabricaAggregate migracao = quarteto.segundo();
        ProcessoConvivenciaTransicaoAggregate transicao = quarteto.terceiro();
        PjbGovernancaInstitucionalNormativaAggregate governanca = quarteto.quarto();
        List<PjbSubstituicaoLegadosProva> provas = List.of(
                prova("producao.pesada", "Prova de produção pesada", producao.statusGeral(), producao.scoreGeral(), producao.prontoProducaoPesada(), producao.fundamentos(), producao.bloqueios()),
                prova("migracao.factory", "Fábrica de migração", migracao.statusGeral(), migracao.scoreGeral(), migracao.prontoMigracao(), migracao.fundamentos(), migracao.bloqueios()),
                prova("convivencia.transicao", "Camada de convivência e transição", transicao.statusGeral(), transicao.scoreGeral(), transicao.prontoShadowMode() && transicao.prontoReversibilidade(), transicao.fundamentos(), List.of()),
                prova("governanca.institucional", "Governança institucional e normativa", governanca.statusGeral(), governanca.scoreGeral(), governanca.prontoGovernanca(), governanca.fundamentos(), governanca.pendencias()),
                prova("blocos.soberanos", "Blocos soberanos em runtime real", blocos.statusGeral(), blocos.scoreGeral(), blocos.prontoRuntimeSoberano(), blocos.fundamentos(), List.of())
        );
        int scoreGeral = (int) Math.round(provas.stream().mapToInt(PjbSubstituicaoLegadosProva::score).average().orElse(0));
        boolean pronto = provas.stream().allMatch(PjbSubstituicaoLegadosProva::concluida) && scoreGeral >= 85;
        String conclusao = pronto
                ? "O PJB atingiu prontidão para substituição imediata sob governança operacional controlada."
                : "O PJB já tem arquitetura para superar os legados; o que falta é prova operacional, migração e governança para substituir os legados.";
        List<PjbSubstituicaoLegadosSistema> sistemas = List.of(
                sistema("PJe", provas, 88, pronto),
                sistema("e-SAJ", provas, 84, pronto),
                sistema("Creta", provas, 82, pronto),
                sistema("Projudi", provas, 83, pronto),
                sistema("eproc", provas, 86, pronto)
        );
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        provas.forEach(item -> fundamentos.add(item.codigo() + ".status=" + item.status().name()));
        fundamentos.add("scoreGeral=" + scoreGeral);
        fundamentos.add("prontoSubstituicao=" + pronto);
        fundamentos.addAll(blocos.fundamentos());
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("plataforma.substituicao.legados", "Processo", String.valueOf(processoId), BigDecimal.valueOf(scoreGeral), provas.toString(), sistemas.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(provas.toString()), "PJB-SUBSTITUICAO", pronto ? "PRONTO" : "CANDIDATO");
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("PJB_SUBSTITUICAO_LEGADOS_AVALIADA", "Processo", String.valueOf(processoId), Hashes.sha256Hex(provas.toString()), "score=" + scoreGeral + ";pronto=" + pronto);
        }
        return new PjbSubstituicaoLegadosAggregate(
                processoId,
                contexto.numeroReferencia(),
                provas,
                sistemas,
                scoreGeral,
                pronto,
                conclusao,
                List.copyOf(fundamentos.stream().limit(180).toList()),
                Instant.now()
        );
    }

    private PjbSubstituicaoLegadosProva prova(String codigo,
                                              String titulo,
                                              PjbFechamentoStatus status,
                                              int score,
                                              boolean concluida,
                                              List<String> fundamentos,
                                              List<String> bloqueios) {
        return new PjbSubstituicaoLegadosProva(codigo, titulo, status, score, concluida, fundamentos, bloqueios);
    }

    private PjbSubstituicaoLegadosSistema sistema(String nome,
                                                  List<PjbSubstituicaoLegadosProva> provas,
                                                  int base,
                                                  boolean pronto) {
        int score = Math.max(0, Math.min(100, (int) Math.round(base * 0.4d + provas.stream().mapToInt(PjbSubstituicaoLegadosProva::score).average().orElse(0) * 0.6d)));
        List<String> pendencias = provas.stream().filter(item -> !item.concluida()).map(PjbSubstituicaoLegadosProva::titulo).toList();
        PjbFechamentoStatus status = pronto ? PjbFechamentoStatus.CONCLUIDA : score >= 70 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        String conclusao = pronto ? "Substituição nacional pronta sob rollout governado." : "Sucessor-alvo viável, dependente das provas de fechamento restantes.";
        return new PjbSubstituicaoLegadosSistema(nome, status, score, conclusao, pendencias);
    }
}
