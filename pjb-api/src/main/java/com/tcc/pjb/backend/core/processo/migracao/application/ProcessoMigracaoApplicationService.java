package com.tcc.pjb.backend.core.processo.migracao.application;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoLegacyMirror;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoIdentity;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoShadowComparison;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoMigracaoApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    public ProcessoMigracaoApplicationService(ProcessoRepository processoRepository,
                                              ProcessoIntegracaoApplicationService processoIntegracaoApplicationService,
                                              ProcessoTimelineApplicationService processoTimelineApplicationService,
                                              ProcessoUnificadoApplicationService processoUnificadoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoIntegracaoApplicationService = Objects.requireNonNull(processoIntegracaoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
    }

    public ProcessoMigracaoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        ProcessoIntegracaoAggregate integracao = processoIntegracaoApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        List<ProcessoShadowComparison> comparacoes = buildComparacoes(processo, integracao, timeline, unificado);
        List<ProcessoLegacyMirror> mirrors = buildMirrors(processo, integracao, comparacoes);
        boolean canCutOver = integracao.prontidaoEnvio().equals("READY")
                && integracao.prontidaoShadow().equals("READY")
                && comparacoes.stream().noneMatch(ProcessoShadowComparison::blocking)
                && timeline.totalBloqueantes() == 0
                && unificado.diagnostico().blockingFindings() == 0;
        String readiness = canCutOver ? "READY_FOR_CUTOVER" : integracao.prontidaoShadow().equals("READY") ? "READY_FOR_SHADOW" : "FOUNDATION_REQUIRED";
        LinkedHashSet<String> proximasOndas = new LinkedHashSet<>();
        if (!integracao.prontidaoShadow().equals("READY")) {
            proximasOndas.add("ATIVAR_DRY_RUN_E_SINCRONIZACAO_COMPARATIVA");
        }
        if (timeline.totalBloqueantes() > 0) {
            proximasOndas.add("ELIMINAR_PENDENCIAS_BLOQUEANTES_ANTES_DO_SHADOW");
        }
        if (comparacoes.stream().anyMatch(ProcessoShadowComparison::blocking)) {
            proximasOndas.add("CORRIGIR_DIVERGENCIAS_DE_SOMBRA_E_COERENCIA");
        }
        if (!canCutOver) {
            proximasOndas.add("MANTER_CONVIVENCIA_COM_LEGADO_COM_RECONCILIACAO_CONTROLADA");
        }
        LinkedHashSet<String> alertas = new LinkedHashSet<>(integracao.alertas());
        if (processo.getConnectorSystem() == null || processo.getConnectorSystem().isBlank()) {
            alertas.add("Sistema legado atual não foi explicitamente materializado no processo.");
        }
        if (!canCutOver) {
            alertas.add("O processo ainda não está pronto para corte total do sistema legado.");
        }
        return new ProcessoMigracaoAggregate(
                identity(processo, integracao),
                readiness,
                canCutOver,
                mirrors,
                comparacoes,
                List.copyOf(proximasOndas),
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private List<ProcessoShadowComparison> buildComparacoes(Processo processo,
                                                            ProcessoIntegracaoAggregate integracao,
                                                            ProcessoTimelineAggregate timeline,
                                                            ProcessoUnificadoAggregate unificado) {
        ArrayList<ProcessoShadowComparison> comparacoes = new ArrayList<>();
        String targetSystem = integracao.trilhaConnector();
        String actualSystem = processo.getConnectorSystem() == null || processo.getConnectorSystem().isBlank()
                ? "NAO_DEFINIDO"
                : processo.getConnectorSystem().trim().toUpperCase(Locale.ROOT);
        comparacoes.add(new ProcessoShadowComparison(
                "SISTEMA_ALVO",
                "Alinhamento entre sistema legado atual e conector nacional sugerido",
                Objects.equals(targetSystem, actualSystem) ? "CONTROLADA" : "ELEVADA",
                false,
                targetSystem,
                actualSystem,
                List.of("O processo precisa conhecer o sistema legado atual e o alvo de shadow mode.")
        ));
        String tribunalMaterializado = firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado());
        String unidadeMaterializada = firstNonBlank(processo.getVara(), processo.getUnidadeJudiciariaCodigo());
        comparacoes.add(new ProcessoShadowComparison(
                "COMPETENCIA_MATERIALIZADA",
                "Competência territorial e unidade materializadas",
                hasText(tribunalMaterializado) && hasText(unidadeMaterializada) ? "CONTROLADA" : "CRITICA",
                !hasText(tribunalMaterializado) || !hasText(unidadeMaterializada),
                "TRIBUNAL_E_UNIDADE_PREENCHIDOS",
                (hasText(tribunalMaterializado) ? tribunalMaterializado : "SEM_TRIBUNAL") + "/" + (hasText(unidadeMaterializada) ? unidadeMaterializada : "SEM_UNIDADE"),
                List.of("Shadow mode e roteamento exigem materialização explícita do tribunal e da unidade judiciária.")
        ));
        comparacoes.add(new ProcessoShadowComparison(
                "COERENCIA_PROCESSUAL",
                "Saúde do motor de coerência processual",
                unificado.diagnostico().healthy() ? "CONTROLADA" : "CRITICA",
                !unificado.diagnostico().healthy(),
                "SEM_ACHADOS_BLOQUEANTES",
                "blockingFindings=" + unificado.diagnostico().blockingFindings(),
                unificado.diagnostico().fundamentos()
        ));
        comparacoes.add(new ProcessoShadowComparison(
                "BLOQUEIOS_OPERACIONAIS",
                "Pendências bloqueantes na linha do tempo viva",
                timeline.totalBloqueantes() == 0 ? "CONTROLADA" : "CRITICA",
                timeline.totalBloqueantes() > 0,
                "0",
                String.valueOf(timeline.totalBloqueantes()),
                timeline.alertas()
        ));
        comparacoes.add(new ProcessoShadowComparison(
                "PRONTIDAO_DE_INTEGRACAO",
                "Prontidão dos canais de envio e shadow",
                integracao.prontidaoEnvio().equals("READY") && integracao.prontidaoShadow().equals("READY") ? "CONTROLADA" : "ELEVADA",
                !integracao.prontidaoShadow().equals("READY"),
                "READY/READY",
                integracao.prontidaoEnvio() + '/' + integracao.prontidaoShadow(),
                merge(integracao.alertas(), integracao.proximasAcoes())
        ));
        return List.copyOf(comparacoes);
    }

    private List<ProcessoLegacyMirror> buildMirrors(Processo processo,
                                                    ProcessoIntegracaoAggregate integracao,
                                                    List<ProcessoShadowComparison> comparacoes) {
        ArrayList<ProcessoLegacyMirror> mirrors = new ArrayList<>();
        String sistemaAtual = processo.getConnectorSystem() == null || processo.getConnectorSystem().isBlank()
                ? integracao.trilhaConnector()
                : processo.getConnectorSystem().trim().toUpperCase(Locale.ROOT);
        mirrors.add(new ProcessoLegacyMirror(
                "LEGADO_ATUAL",
                "Espelho do sistema legado atual",
                sistemaAtual,
                "DUAL_READ",
                true,
                toInstant(processo.getConnectorSnapshotSyncedAt()),
                integracao.prontidaoShadow(),
                comparacoes.stream().filter(ProcessoShadowComparison::blocking).map(ProcessoShadowComparison::codigo).toList()
        ));
        mirrors.add(new ProcessoLegacyMirror(
                "PJB_CANONICO",
                "Espelho canônico do PJB para corte gradual",
                "PJB",
                "CANONICAL_WRITE",
                true,
                toInstant(processo.getDataUltimaMovimentacao()),
                integracao.prontidaoEnvio(),
                comparacoes.stream().filter(item -> !item.blocking()).map(ProcessoShadowComparison::codigo).toList()
        ));
        return List.copyOf(mirrors);
    }

    private ProcessoMigracaoIdentity identity(Processo processo, ProcessoIntegracaoAggregate integracao) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado()) != null) marcadores.add(firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado()));
        if (processo.getRamoDireito() != null) marcadores.add(processo.getRamoDireito().name());
        if (processo.getRito() != null) marcadores.add(processo.getRito().name());
        marcadores.add(integracao.trilhaConnector());
        return new ProcessoMigracaoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado()),
                firstNonBlank(processo.getVara(), processo.getUnidadeJudiciariaCodigo()),
                processo.getConnectorSystem() == null || processo.getConnectorSystem().isBlank() ? "NAO_DEFINIDO" : processo.getConnectorSystem().trim().toUpperCase(Locale.ROOT),
                integracao.trilhaConnector(),
                List.copyOf(marcadores)
        );
    }

    @SafeVarargs
    private final List<String> merge(List<String>... values) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (List<String> value : values) {
            if (value != null) {
                value.stream().filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).forEach(merged::add);
            }
        }
        return List.copyOf(merged);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }
}
