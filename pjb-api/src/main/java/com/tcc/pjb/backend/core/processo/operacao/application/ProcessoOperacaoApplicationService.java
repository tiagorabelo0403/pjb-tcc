package com.tcc.pjb.backend.core.processo.operacao.application;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoFaixa;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoIdentity;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.model.dto.processo.ProcessoAcessoVisibilidadeResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoOperacaoApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;
    private final ProcessoMigracaoApplicationService processoMigracaoApplicationService;
    private final ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService;

    public ProcessoOperacaoApplicationService(ProcessoRepository processoRepository,
                                              ProcessoTimelineApplicationService processoTimelineApplicationService,
                                              ProcessoIntegracaoApplicationService processoIntegracaoApplicationService,
                                              ProcessoMigracaoApplicationService processoMigracaoApplicationService,
                                              ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoIntegracaoApplicationService = Objects.requireNonNull(processoIntegracaoApplicationService);
        this.processoMigracaoApplicationService = Objects.requireNonNull(processoMigracaoApplicationService);
        this.processoObservabilidadeAcessoService = Objects.requireNonNull(processoObservabilidadeAcessoService);
    }

    public ProcessoOperacaoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoIntegracaoAggregate integracao = processoIntegracaoApplicationService.detalhar(processoId);
        ProcessoMigracaoAggregate migracao = processoMigracaoApplicationService.detalhar(processoId);
        ProcessoAcessoVisibilidadeResponse visibilidade = processoObservabilidadeAcessoService.resumir(processo);

        ArrayList<ProcessoOperacaoFaixa> faixas = new ArrayList<>();
        faixas.add(faixaFluxo(timeline));
        faixas.add(faixaObservabilidade(visibilidade));
        faixas.add(faixaIntegracao(integracao));
        faixas.add(faixaMigracao(migracao));

        LinkedHashSet<String> acoes = new LinkedHashSet<>();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        double saturacaoMaxima = 0d;
        long totalBloqueios = 0L;
        for (ProcessoOperacaoFaixa faixa : faixas) {
            saturacaoMaxima = Math.max(saturacaoMaxima, faixa.saturacao());
            totalBloqueios += Math.max(0L, faixa.bloqueios());
            acoes.addAll(faixa.acoesImediatas());
            if (!"STABLE".equals(faixa.estado())) {
                alertas.add(faixa.titulo() + ": " + String.join(" | ", faixa.detalhes()));
            }
        }
        timeline.alertas().forEach(alertas::add);
        integracao.alertas().forEach(alertas::add);
        migracao.alertas().forEach(alertas::add);
        visibilidade.mensagens().forEach(alertas::add);

        return new ProcessoOperacaoAggregate(
                identity(processo),
                readiness(faixas),
                resilienceState(faixas),
                observabilityState(faixas),
                migracao.readiness(),
                round(saturacaoMaxima),
                totalBloqueios,
                List.copyOf(faixas),
                List.copyOf(acoes),
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private ProcessoOperacaoFaixa faixaFluxo(ProcessoTimelineAggregate timeline) {
        double saturacao = normalized(timeline.totalBloqueantes() * 18d + timeline.totalPendencias() * 4d + timeline.alertas().size() * 6d);
        String estado = timeline.totalBloqueantes() > 0 ? "CRITICAL" : timeline.alertas().isEmpty() ? "STABLE" : "ATTENTION";
        ArrayList<String> detalhes = new ArrayList<>();
        detalhes.add("eventos=" + timeline.totalEventos());
        detalhes.add("pendencias=" + timeline.totalPendencias());
        detalhes.add("bloqueantes=" + timeline.totalBloqueantes());
        if (!timeline.proximoCiclo().isEmpty()) {
            detalhes.add("proximo=" + timeline.proximoCiclo().getFirst());
        }
        LinkedHashSet<String> acoes = new LinkedHashSet<>(timeline.proximoCiclo());
        if (timeline.totalBloqueantes() > 0) {
            acoes.add("DESOBSTRUIR_FLUXO_PROCESSUAL");
        }
        return new ProcessoOperacaoFaixa("FLUXO_VIVO", "Fluxo vivo e filas críticas", "OPERACAO", estado, saturacao, timeline.totalBloqueantes(), detalhes, List.copyOf(acoes));
    }

    private ProcessoOperacaoFaixa faixaObservabilidade(ProcessoAcessoVisibilidadeResponse visibilidade) {
        long bloqueios = visibilidade.ultimaLeituraInstitucionalAt() == null ? 1L : 0L;
        boolean semResponsabilidade = visibilidade.responsabilidadesAtuais() == null || visibilidade.responsabilidadesAtuais().isEmpty();
        if (semResponsabilidade) {
            bloqueios++;
        }
        String estado = bloqueios >= 2 ? "CRITICAL" : bloqueios == 1 ? "ATTENTION" : "STABLE";
        double saturacao = normalized(bloqueios * 30d + sizeOf(visibilidade.categorias()) * 2d + sizeOf(visibilidade.papeisAtivos()));
        ArrayList<String> detalhes = new ArrayList<>();
        detalhes.add("categorias=" + sizeOf(visibilidade.categorias()));
        detalhes.add("responsabilidades=" + sizeOf(visibilidade.responsabilidadesAtuais()));
        detalhes.add("papeisAtivos=" + sizeOf(visibilidade.papeisAtivos()));
        detalhes.add(visibilidade.ultimaLeituraInstitucionalAt() == null ? "semLeituraInstitucional" : "leituraInstitucionalPresente");
        LinkedHashSet<String> acoes = new LinkedHashSet<>();
        if (visibilidade.ultimaLeituraInstitucionalAt() == null) {
            acoes.add("REGISTRAR_PRIMEIRA_LEITURA_INSTITUCIONAL");
        }
        if (semResponsabilidade) {
            acoes.add("MATERIALIZAR_RESPONSAVEL_ATUAL_NA_CAIXA");
        }
        if (sizeOf(visibilidade.papeisAtivos()) == 0) {
            acoes.add("ATIVAR_PAPEIS_E_RESPONSAVEIS_DO_PROCESSO");
        }
        return new ProcessoOperacaoFaixa("OBSERVABILIDADE", "Observabilidade, posse e rastreio", "OBSERVABILIDADE", estado, saturacao, bloqueios, detalhes, List.copyOf(acoes));
    }

    private ProcessoOperacaoFaixa faixaIntegracao(ProcessoIntegracaoAggregate integracao) {
        long bloqueios = 0L;
        if (!"READY".equalsIgnoreCase(integracao.prontidaoEnvio())) {
            bloqueios++;
        }
        if (!"READY".equalsIgnoreCase(integracao.prontidaoShadow())) {
            bloqueios++;
        }
        double saturacao = normalized(bloqueios * 28d + integracao.alertas().size() * 7d + integracao.proximasAcoes().size() * 3d);
        String estado = bloqueios >= 2 ? "CRITICAL" : bloqueios == 1 ? "ATTENTION" : "STABLE";
        ArrayList<String> detalhes = new ArrayList<>();
        detalhes.add("connector=" + upper(integracao.trilhaConnector()));
        detalhes.add("envio=" + upper(integracao.prontidaoEnvio()));
        detalhes.add("shadow=" + upper(integracao.prontidaoShadow()));
        detalhes.add("canais=" + integracao.canais().size());
        return new ProcessoOperacaoFaixa("INTEGRACAO", "Integração externa e shadow mode", "INTEGRACAO", estado, saturacao, bloqueios, detalhes, integracao.proximasAcoes());
    }

    private ProcessoOperacaoFaixa faixaMigracao(ProcessoMigracaoAggregate migracao) {
        long bloqueios = migracao.canCutOver() ? 0L : 1L;
        double saturacao = normalized(bloqueios * 24d + migracao.alertas().size() * 8d + migracao.comparacoes().size() * 2d);
        String estado = migracao.canCutOver() ? "STABLE" : "ATTENTION";
        ArrayList<String> detalhes = new ArrayList<>();
        detalhes.add("readiness=" + upper(migracao.readiness()));
        detalhes.add("mirrors=" + migracao.mirrors().size());
        detalhes.add("comparacoes=" + migracao.comparacoes().size());
        detalhes.add(migracao.canCutOver() ? "cutoverLiberado" : "cutoverBloqueado");
        return new ProcessoOperacaoFaixa("MIGRACAO", "Convivência, corte e estabilidade de legado", "MIGRACAO", estado, saturacao, bloqueios, detalhes, migracao.proximasOndas());
    }

    private ProcessoOperacaoIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        add(marcadores, processo.getTribunal());
        add(marcadores, safeName(processo.getRamoDireito()));
        add(marcadores, safeName(processo.getRito()));
        add(marcadores, safeName(processo.getFaseAtual()));
        add(marcadores, safeName(processo.getStatusProcesso()));
        add(marcadores, safeName(processo.getNivelSigilo()));
        return new ProcessoOperacaoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getTribunal(),
                processo.getVara(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                List.copyOf(marcadores)
        );
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));
    }

    private String readiness(List<ProcessoOperacaoFaixa> faixas) {
        long critical = faixas.stream().filter(item -> "CRITICAL".equals(item.estado())).count();
        long attention = faixas.stream().filter(item -> "ATTENTION".equals(item.estado())).count();
        if (critical > 0) {
            return "NOT_READY";
        }
        if (attention > 0) {
            return "PARTIAL_READY";
        }
        return "READY";
    }

    private String resilienceState(List<ProcessoOperacaoFaixa> faixas) {
        return faixas.stream().map(ProcessoOperacaoFaixa::estado).anyMatch("CRITICAL"::equals) ? "FRAGIL" : faixas.stream().map(ProcessoOperacaoFaixa::estado).anyMatch("ATTENTION"::equals) ? "OBSERVAR" : "FORTE";
    }

    private String observabilityState(List<ProcessoOperacaoFaixa> faixas) {
        return faixas.stream().filter(item -> "OBSERVABILIDADE".equals(item.eixo())).findFirst().map(ProcessoOperacaoFaixa::estado).orElse("ATTENTION");
    }

    private double normalized(double value) {
        return round(Math.max(0d, Math.min(100d, value)));
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private long sizeOf(List<?> values) {
        return values == null ? 0L : values.size();
    }

    private void add(LinkedHashSet<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }

    private String upper(String value) {
        return value == null ? "NAO_INFORMADO" : value.trim().toUpperCase(Locale.ROOT);
    }
}
