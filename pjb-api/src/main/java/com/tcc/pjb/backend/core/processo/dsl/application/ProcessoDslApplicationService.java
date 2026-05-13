package com.tcc.pjb.backend.core.processo.dsl.application;

import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslAggregate;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslBlock;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslRule;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslVersion;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoTrilha;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalJanela;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoDslApplicationService {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;

    public ProcessoDslApplicationService(ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                         ProcessoRecursalApplicationService processoRecursalApplicationService,
                                         ProcessoExecucaoApplicationService processoExecucaoApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoRecursalApplicationService = Objects.requireNonNull(processoRecursalApplicationService);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
    }

    public ProcessoDslAggregate detalhar(Long processoId) {
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoRecursalAggregate recursal = processoRecursalApplicationService.detalhar(processoId);
        ProcessoExecucaoAggregate execucao = processoExecucaoApplicationService.detalhar(processoId);
        ProcessoDslVersion version = versionar(unificado, recursal, execucao);
        List<ProcessoDslBlock> blocks = List.of(
                blocoCompetencia(unificado, version),
                blocoFluxoBase(unificado, version),
                blocoRecursal(recursal, version),
                blocoExecucao(execucao, version)
        );
        long totalRules = blocks.stream().mapToLong(block -> block.rules().size()).sum();
        long blockingRules = blocks.stream().flatMap(block -> block.rules().stream()).filter(ProcessoDslRule::blocking).count();
        return new ProcessoDslAggregate(
                unificado.identity(),
                version,
                totalRules,
                blockingRules,
                blocks,
                invariants(unificado, recursal, execucao),
                Instant.now()
        );
    }

    private ProcessoDslVersion versionar(ProcessoUnificadoAggregate unificado,
                                         ProcessoRecursalAggregate recursal,
                                         ProcessoExecucaoAggregate execucao) {
        LinkedHashSet<String> axes = new LinkedHashSet<>();
        axes.add("COMPETENCIA");
        axes.add("FLUXO_BASE");
        if (recursal.totalCabiveis() > 0) {
            axes.add("RECURSAL");
        }
        if (execucao.totalTrilhas() > 0) {
            axes.add("EXECUCAO");
        }
        if (execucao.totalOperacoesCustodia() > 0) {
            axes.add("CUSTODIA");
        }
        LocalDate effectiveFrom = LocalDate.of(2026, 1, 1);
        LocalDate effectiveUntil = unificado.competencia().statusProcessual().contains("ARQUIVADO") ? LocalDate.now() : null;
        return new ProcessoDslVersion(
                "PJB_PROCESSUAL_DSL_" + safeSegment(unificado.competencia().ramoDireito()) + "_" + safeSegment(unificado.competencia().ritoProcessual()),
                "2026.1",
                effectiveFrom,
                effectiveUntil,
                "CATALOGO_VERSIONADO_E_EXPLICAVEL",
                List.copyOf(axes)
        );
    }

    private ProcessoDslBlock blocoCompetencia(ProcessoUnificadoAggregate unificado, ProcessoDslVersion version) {
        List<ProcessoDslRule> rules = new ArrayList<>();
        rules.add(new ProcessoDslRule(
                "COMPETENCIA_ENVELOPE_BASE",
                "COMPETENCIA",
                "Enforce competência material, territorial e funcional",
                "competenciaEnvelope != null && tribunalCodigo != null && unidadeJudiciariaCodigo != null",
                "ALLOW",
                profiles("MAGISTRADO", "DISTRIBUIDOR", "SERVIDOR_SECRETARIA"),
                unificado.competencia().reviewChecklist(),
                false,
                version.effectiveFrom(),
                version.effectiveUntil()
        ));
        if (unificado.diagnostico().blockingFindings() > 0) {
            rules.add(new ProcessoDslRule(
                    "COMPETENCIA_COM_ACHADO_BLOQUEANTE",
                    "COMPETENCIA",
                    "Block distribuição incoerente com achados bloqueantes",
                    "diagnostico.blockingFindings > 0",
                    "BLOCK",
                    profiles("DISTRIBUIDOR", "SERVIDOR_SECRETARIA"),
                    List.of("REVISAR_DIAGNOSTICO_UNIFICADO", "VALIDAR_CLASSE_ASSUNTO_TRIBUNAL"),
                    true,
                    version.effectiveFrom(),
                    version.effectiveUntil()
            ));
        }
        return new ProcessoDslBlock(
                "COMPETENCIA",
                "DSL de competência e distribuição",
                version.semanticVersion(),
                rules,
                List.of("ProcessoUnificadoApplicationService", "NationalProcessRoutingService")
        );
    }

    private ProcessoDslBlock blocoFluxoBase(ProcessoUnificadoAggregate unificado, ProcessoDslVersion version) {
        List<ProcessoDslRule> rules = new ArrayList<>();
        unificado.atosPermitidos().stream().limit(8).forEach(ato -> rules.add(toFluxoRule(ato, version)));
        if (rules.isEmpty()) {
            rules.add(new ProcessoDslRule(
                    "FLUXO_BASE_SEM_ATOS_PERMITIDOS",
                    "FLUXO_BASE",
                    "Block salvamento quando não há ato processual compatível",
                    "atosPermitidos.isEmpty()",
                    "BLOCK",
                    profiles("MAGISTRADO", "SERVIDOR_SECRETARIA", "ASSESSOR"),
                    List.of("REABRIR_FLUXO", "REVISAR_STATUS_PROCESSUAL"),
                    true,
                    version.effectiveFrom(),
                    version.effectiveUntil()
            ));
        }
        return new ProcessoDslBlock(
                "FLUXO_BASE",
                "DSL de fluxo base do processo",
                version.semanticVersion(),
                rules,
                List.of("ProcessoUnificadoApplicationService", "ProcessoLifecycleMachine")
        );
    }

    private ProcessoDslRule toFluxoRule(ProcessoUnificadoAto ato, ProcessoDslVersion version) {
        return new ProcessoDslRule(
                ato.codigo(),
                normalizeAxis(ato.eixoOperacional()),
                ato.titulo(),
                "fase/status compatíveis && guardrails satisfeitos && fila='" + ato.filaPadrao() + "'",
                ato.permitido() ? "ALLOW" : "BLOCK",
                profiles(ato.responsavelSugerido()),
                merge(ato.alertas(), List.of(ato.transitionKey(), ato.inboxPadrao(), ato.workItemType())),
                !ato.permitido() || ato.exigeSegurancaElevada(),
                version.effectiveFrom(),
                version.effectiveUntil()
        );
    }

    private ProcessoDslBlock blocoRecursal(ProcessoRecursalAggregate recursal, ProcessoDslVersion version) {
        List<ProcessoDslRule> rules = recursal.janelas().stream().limit(10).map(janela -> new ProcessoDslRule(
                janela.codigo(),
                normalizeAxis(janela.eixo()),
                janela.titulo(),
                "rota='" + janela.rota() + "' && tribunalDestino='" + janela.tribunalDestino() + "'",
                janela.cabivel() ? "ALLOW" : "BLOCK",
                profiles(janela.autoridadeAdmissibilidade(), janela.autoridadeMerito()),
                merge(janela.guardas(), merge(janela.eventosIniciais(), janela.fundamentos())),
                !janela.cabivel(),
                version.effectiveFrom(),
                version.effectiveUntil()
        )).toList();
        return new ProcessoDslBlock(
                "RECURSAL",
                "DSL recursal e de embargos",
                version.semanticVersion(),
                rules,
                List.of("ProcessoRecursalApplicationService")
        );
    }

    private ProcessoDslBlock blocoExecucao(ProcessoExecucaoAggregate execucao, ProcessoDslVersion version) {
        List<ProcessoDslRule> rules = execucao.trilhas().stream().limit(10).map(trilha -> new ProcessoDslRule(
                trilha.codigo(),
                normalizeAxis(trilha.eixo()),
                trilha.titulo(),
                "fila='" + trilha.fila() + "' && inbox='" + trilha.inbox() + "'",
                "ALLOW",
                profiles(trilha.papelResponsavel()),
                merge(trilha.guardas(), merge(trilha.mandados(), trilha.operacoesCustodia())),
                trilha.bloqueante(),
                version.effectiveFrom(),
                version.effectiveUntil()
        )).toList();
        return new ProcessoDslBlock(
                "EXECUCAO",
                "DSL executiva, mandados e custódia",
                version.semanticVersion(),
                rules,
                List.of("ProcessoExecucaoApplicationService")
        );
    }

    private List<String> invariants(ProcessoUnificadoAggregate unificado,
                                    ProcessoRecursalAggregate recursal,
                                    ProcessoExecucaoAggregate execucao) {
        LinkedHashSet<String> invariants = new LinkedHashSet<>();
        invariants.add("ATO_PROCESSUAL_NASCE_DE_RITO_FASE_STATUS_E_COMPETENCIA");
        invariants.add("PERFIL_OPERACIONAL_NAO_SUPERA_GUARDA_DE_DOMINIO");
        invariants.add("VERSAO_DA_DSL_PRESERVA_EXPLICABILIDADE_DA_REGRA");
        if (recursal.totalCabiveis() > 0) {
            invariants.add("TRILHA_RECURSAL_PERMANECE_SEPARADA_DO_FLUXO_BASE");
        }
        if (execucao.totalTrilhas() > 0) {
            invariants.add("EXECUCAO_E_CUSTODIA_NAO_SE_MISTURAM_COM_GOVERNANCA");
        }
        if (unificado.diagnostico().blockingFindings() > 0) {
            invariants.add("ACHADO_BLOQUEANTE_IMPEDE_REGRA_SILENCIOSA");
        }
        return List.copyOf(invariants);
    }

    private List<String> merge(List<String> first, List<String> second) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (first != null) {
            values.addAll(first);
        }
        if (second != null) {
            values.addAll(second);
        }
        values.removeIf(value -> value == null || value.isBlank());
        return List.copyOf(values);
    }


    private List<String> profiles(String... values) {
        LinkedHashSet<String> profiles = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    profiles.add(value);
                }
            }
        }
        return List.copyOf(profiles);
    }

    private String normalizeAxis(String value) {
        if (value == null || value.isBlank()) {
            return "GERAL";
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String safeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "GERAL";
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
