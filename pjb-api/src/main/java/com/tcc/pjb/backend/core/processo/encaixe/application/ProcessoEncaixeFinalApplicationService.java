package com.tcc.pjb.backend.core.processo.encaixe.application;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeCarteiraAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinding;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeResumo;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelAggregate;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoFinding;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProcessoEncaixeFinalApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    private final ProcessoPrazoApplicationService processoPrazoApplicationService;
    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final ProcessoPapelApplicationService processoPapelApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;
    private final ProcessoMigracaoApplicationService processoMigracaoApplicationService;
    private final ProcessoOperacaoApplicationService processoOperacaoApplicationService;

    public ProcessoEncaixeFinalApplicationService(ProcessoRepository processoRepository,
                                                  ProcessoUnificadoApplicationService processoUnificadoApplicationService,
                                                  ProcessoPrazoApplicationService processoPrazoApplicationService,
                                                  ProcessoTrabalhoApplicationService processoTrabalhoApplicationService,
                                                  ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                                  ProcessoRecursalApplicationService processoRecursalApplicationService,
                                                  ProcessoExecucaoApplicationService processoExecucaoApplicationService,
                                                  ProcessoPapelApplicationService processoPapelApplicationService,
                                                  ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                  ProcessoIntegracaoApplicationService processoIntegracaoApplicationService,
                                                  ProcessoMigracaoApplicationService processoMigracaoApplicationService,
                                                  ProcessoOperacaoApplicationService processoOperacaoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
        this.processoPrazoApplicationService = Objects.requireNonNull(processoPrazoApplicationService);
        this.processoTrabalhoApplicationService = Objects.requireNonNull(processoTrabalhoApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.processoRecursalApplicationService = Objects.requireNonNull(processoRecursalApplicationService);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
        this.processoPapelApplicationService = Objects.requireNonNull(processoPapelApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoIntegracaoApplicationService = Objects.requireNonNull(processoIntegracaoApplicationService);
        this.processoMigracaoApplicationService = Objects.requireNonNull(processoMigracaoApplicationService);
        this.processoOperacaoApplicationService = Objects.requireNonNull(processoOperacaoApplicationService);
    }

    public ProcessoEncaixeFinalAggregate detalhar(Long processoId) {
        Processo processo = load(processoId);
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        ProcessoPrazoAggregate prazo = processoPrazoApplicationService.detalhar(processoId);
        ProcessoTrabalhoAggregate trabalho = processoTrabalhoApplicationService.detalhar(processoId);
        ProcessoDocumentoAggregate documental = processoDocumentoApplicationService.detalhar(processoId);
        ProcessoRecursalAggregate recursal = processoRecursalApplicationService.detalhar(processoId);
        ProcessoExecucaoAggregate execucao = processoExecucaoApplicationService.detalhar(processoId);
        ProcessoPapelAggregate papel = processoPapelApplicationService.detalhar(processoId);
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoIntegracaoAggregate integracao = processoIntegracaoApplicationService.detalhar(processoId);
        ProcessoMigracaoAggregate migracao = processoMigracaoApplicationService.detalhar(processoId);
        ProcessoOperacaoAggregate operacao = processoOperacaoApplicationService.detalhar(processoId);

        ArrayList<ProcessoEncaixeFinding> findings = new ArrayList<>();
        scanProcessoBase(processo, findings);
        scanUnificado(unificado, findings);
        scanPrazo(processo, prazo, findings);
        scanTrabalho(processo, trabalho, findings);
        scanDocumental(processo, documental, findings);
        scanRecursal(processo, recursal, findings);
        scanExecucao(processo, execucao, findings);
        scanPapel(papel, findings);
        scanTimeline(timeline, findings);
        scanIntegracao(integracao, migracao, findings);
        scanOperacao(operacao, findings);

        findings.sort(Comparator.comparing(ProcessoEncaixeFinding::bloqueante).reversed()
                .thenComparing(this::severityRank)
                .thenComparing(ProcessoEncaixeFinding::codigo));

        long bloqueantes = findings.stream().filter(ProcessoEncaixeFinding::bloqueante).count();
        long score = score(findings, operacao);
        LinkedHashSet<String> eixos = new LinkedHashSet<>();
        LinkedHashSet<String> acoes = new LinkedHashSet<>();
        for (ProcessoEncaixeFinding finding : findings) {
            eixos.add(finding.eixo());
            if (!finding.remediacao().isBlank()) {
                acoes.add(finding.remediacao());
            }
        }
        acoes.addAll(operacao.acoesImediatas());
        return new ProcessoEncaixeFinalAggregate(
                processo.getId(),
                processo.getNumeroProcesso(),
                readiness(score, bloqueantes),
                score,
                findings.size(),
                bloqueantes,
                List.copyOf(eixos),
                findings,
                List.copyOf(acoes),
                Instant.now()
        );
    }

    public ProcessoEncaixeCarteiraAggregate varrer(int limite) {
        int capped = Math.max(1, Math.min(50, limite));
        List<Processo> processos = processoRepository.findAll(PageRequest.of(0, capped, Sort.by(Sort.Direction.ASC, "id"))).getContent();
        ArrayList<ProcessoEncaixeResumo> resumos = new ArrayList<>();
        LinkedHashMap<String, FindingAccumulator> acumuladores = new LinkedHashMap<>();
        long totalBloqueantes = 0L;
        long scoreSoma = 0L;
        for (Processo processo : processos) {
            ProcessoEncaixeFinalAggregate aggregate = detalhar(processo.getId());
            totalBloqueantes += aggregate.totalBloqueantes();
            scoreSoma += aggregate.score();
            resumos.add(new ProcessoEncaixeResumo(
                    aggregate.processoId(),
                    aggregate.numeroProcesso(),
                    aggregate.readiness(),
                    aggregate.score(),
                    aggregate.totalBloqueantes(),
                    aggregate.totalFindings(),
                    aggregate.findings().stream().limit(3).map(ProcessoEncaixeFinding::codigo).toList()
            ));
            aggregate.findings().forEach(finding -> acumuladores.computeIfAbsent(finding.codigo(), ignored -> new FindingAccumulator(finding)).increment());
        }
        List<ProcessoEncaixeFinding> tendencias = acumuladores.values().stream()
                .sorted(Comparator.comparing(FindingAccumulator::count).reversed().thenComparing(item -> item.finding().codigo()))
                .limit(10)
                .map(item -> new ProcessoEncaixeFinding(
                        item.finding().codigo(),
                        item.finding().titulo(),
                        item.finding().eixo(),
                        item.finding().severidade(),
                        item.finding().bloqueante(),
                        "Ocorrencias na amostra=" + item.count(),
                        item.finding().remediacao()
                ))
                .toList();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (processos.isEmpty()) {
            alertas.add("Nenhum processo disponível para a varredura final.");
        }
        if (totalBloqueantes > 0) {
            alertas.add("A amostra escaneada ainda contém bloqueios estruturais que impedem corte amplo para substituição integral.");
        }
        return new ProcessoEncaixeCarteiraAggregate(
                processos.size(),
                totalBloqueantes,
                processos.isEmpty() ? 0L : Math.round(scoreSoma / (double) processos.size()),
                resumos,
                tendencias,
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private void scanProcessoBase(Processo processo, List<ProcessoEncaixeFinding> findings) {
        if (blank(processo.getNumeroProcesso())) {
            findings.add(finding("PROCESSO_SEM_NUMERO", "Processo sem número materializado", "PROCESSO", "CRITICAL", true, "O processo ainda não consolidou número unificado ou número local.", "MATERIALIZAR_NUMERACAO_PROCESSUAL"));
        }
        if (blank(processo.getTribunal())) {
            findings.add(finding("PROCESSO_SEM_TRIBUNAL", "Processo sem tribunal roteado", "COMPETENCIA", "CRITICAL", true, "Não existe tribunal ou código jurisdicional suficiente para o roteamento nacional.", "DEFINIR_TRIBUNAL_E_UNIDADE"));
        }
        if (blank(processo.getVara())) {
            findings.add(finding("PROCESSO_SEM_UNIDADE", "Processo sem unidade judiciária", "COMPETENCIA", "CRITICAL", true, "A unidade judiciária não foi materializada no processo vivo.", "MATERIALIZAR_UNIDADE_JUDICIARIA"));
        }
        if (processo.getRamoDireito() == null || processo.getRito() == null || processo.getFaseAtual() == null || processo.getStatusProcesso() == null) {
            findings.add(finding("PROCESSO_COM_ESTADO_INCOMPLETO", "Estado processual incompleto", "PROCESSO", "CRITICAL", true, "Ramo, rito, fase ou status não estão completamente definidos para orquestração nacional.", "COMPLETAR_ENVELOPE_PROCESSUAL"));
        }
    }

    private void scanUnificado(ProcessoUnificadoAggregate unificado, List<ProcessoEncaixeFinding> findings) {
        if (unificado.atosPermitidos().isEmpty()) {
            findings.add(finding("SEM_ATOS_PERMITIDOS", "Processo sem atos permitidos", "COERENCIA", "CRITICAL", true, "O motor unificado não encontrou qualquer ato permitido para o contexto atual.", "RECALCULAR_TRILHA_DE_ATOS"));
        }
        unificado.diagnostico().findings().stream().filter(ProcessoUnificadoFinding::blocking).forEach(item ->
                findings.add(finding(item.code(), item.title(), "COERENCIA", "CRITICAL", true, item.detail(), "SANEAR_MOTOR_DE_COERENCIA")));
    }

    private void scanPrazo(Processo processo, ProcessoPrazoAggregate prazo, List<ProcessoEncaixeFinding> findings) {
        boolean ativo = processo.getStatusProcesso() != StatusProcesso.ARQUIVADO && processo.getStatusProcesso() != StatusProcesso.TRANSITO_EM_JULGADO;
        if (ativo && prazo.totalMarcos() == 0) {
            findings.add(finding("PROCESSO_SEM_PRAZOS", "Processo ativo sem marcos de prazo", "PRAZO", "ATTENTION", false, "Não existem marcos processuais materializados para um processo ainda ativo.", "GERAR_MARCOS_PROCESSUAIS"));
        }
        if (prazo.marcosVencidos() > 0) {
            findings.add(finding("PRAZOS_VENCIDOS", "Existem marcos vencidos", "PRAZO", "CRITICAL", true, "Foram encontrados prazos vencidos com impacto operacional.", "TRATAR_MARCOS_VENCIDOS"));
        }
    }

    private void scanTrabalho(Processo processo, ProcessoTrabalhoAggregate trabalho, List<ProcessoEncaixeFinding> findings) {
        boolean ativo = processo.getStatusProcesso() != StatusProcesso.ARQUIVADO;
        if (ativo && trabalho.totalWorkItems() == 0) {
            findings.add(finding("SEM_WORKITEMS", "Processo ativo sem work items", "TRABALHO", "ATTENTION", false, "O processo não possui work items ou caixas materializadas.", "GERAR_WORKITEMS_E_FILAS"));
        }
        if (trabalho.bloqueantes() > 0) {
            findings.add(finding("WORKSTREAM_BLOQUEADO", "Fila operacional bloqueada", "TRABALHO", "CRITICAL", true, "Existem itens bloqueantes impedindo a progressão do fluxo.", "DESBLOQUEAR_CAIXA_PROCESSUAL"));
        }
    }

    private void scanDocumental(Processo processo, ProcessoDocumentoAggregate documental, List<ProcessoEncaixeFinding> findings) {
        boolean ativo = processo.getStatusProcesso() != StatusProcesso.ARQUIVADO;
        if (ativo && documental.totalDocumentos() == 0) {
            findings.add(finding("SEM_DOCUMENTOS", "Processo sem trilha documental", "DOCUMENTAL", "ATTENTION", false, "Nenhum documento ou lote foi materializado para um processo ainda em curso.", "MATERIALIZAR_LOTES_E_DOCUMENTOS"));
        }
        if (documental.minutas() > 0 && documental.assinados() == 0) {
            findings.add(finding("MINUTAS_SEM_ASSINATURA", "Minutas sem fecho assinável", "DOCUMENTAL", "ATTENTION", false, "Existem minutas em aberto sem qualquer documento assinado na trilha.", "CONCLUIR_TRILHA_ASSINAVEL"));
        }
    }

    private void scanRecursal(Processo processo, ProcessoRecursalAggregate recursal, List<ProcessoEncaixeFinding> findings) {
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL && recursal.janelas().isEmpty()) {
            findings.add(finding("RECURSAL_SEM_JANELA", "Fase recursal sem janela materializada", "RECURSAL", "CRITICAL", true, "A fase recursal está ativa, mas não existem janelas recursais ou de embargos calculadas.", "MATERIALIZAR_JANELAS_RECURSAIS"));
        }
        if (!recursal.travas().isEmpty()) {
            findings.add(finding("TRAVAS_RECURSAIS", "Travas recursais presentes", "RECURSAL", "ATTENTION", false, String.join(" | ", recursal.travas()), "REVISAR_ADMISSIBILIDADE_E_PREPARO"));
        }
    }

    private void scanExecucao(Processo processo, ProcessoExecucaoAggregate execucao, List<ProcessoEncaixeFinding> findings) {
        if (containsAny(safeName(processo.getRito()), "EXECUCAO", "EXECUÇÃO") && execucao.totalTrilhas() == 0) {
            findings.add(finding("EXECUCAO_SEM_TRILHA", "Rito executivo sem trilhas", "EXECUCAO", "CRITICAL", true, "O rito executivo está materializado, mas nenhuma trilha de cumprimento foi aberta.", "ABRIR_TRILHA_EXECUTIVA"));
        }
        if (execucao.totalBloqueantes() > 0) {
            findings.add(finding("EXECUCAO_BLOQUEADA", "Trilha executiva com bloqueios", "EXECUCAO", "ATTENTION", false, String.join(" | ", execucao.alertas()), "TRATAR_BLOQUEIOS_EXECUTIVOS"));
        }
    }

    private void scanPapel(ProcessoPapelAggregate papel, List<ProcessoEncaixeFinding> findings) {
        if (papel.totalPerfis() == 0) {
            findings.add(finding("SEM_PERFIS_PROCESSUAIS", "Nenhum perfil processual materializado", "PAPEL", "CRITICAL", true, "O processo não possui matriz de perfis e poderes aplicável.", "MATERIALIZAR_MATRIZ_DE_PAPEIS"));
        }
        if (papel.totalAssinantes() == 0) {
            findings.add(finding("SEM_ASSINANTES", "Sem perfil assinante apto", "PAPEL", "ATTENTION", false, "Nenhum perfil habilitado a assinar foi encontrado na malha processual.", "HABILITAR_PERFIL_ASSINANTE"));
        }
    }

    private void scanTimeline(ProcessoTimelineAggregate timeline, List<ProcessoEncaixeFinding> findings) {
        if (timeline.totalBloqueantes() > 0) {
            findings.add(finding("TIMELINE_COM_BLOQUEIOS", "Linha do tempo com bloqueios", "TIMELINE", "CRITICAL", true, "A timeline do processo expõe pendências bloqueantes antes do próximo ciclo.", "SANEAR_TIMELINE_VIVA"));
        }
        if (timeline.eventos().isEmpty()) {
            findings.add(finding("TIMELINE_VAZIA", "Linha do tempo vazia", "TIMELINE", "ATTENTION", false, "O processo não gerou qualquer evento na timeline viva.", "MATERIALIZAR_EVENTOS_DA_TIMELINE"));
        }
    }

    private void scanIntegracao(ProcessoIntegracaoAggregate integracao, ProcessoMigracaoAggregate migracao, List<ProcessoEncaixeFinding> findings) {
        if ("BLOCKED".equalsIgnoreCase(integracao.prontidaoEnvio())) {
            findings.add(finding("INTEGRACAO_BLOQUEADA", "Integração externa bloqueada", "INTEGRACAO", "ATTENTION", false, "O conector externo ainda não está pronto para submissão.", "REMOVER_BLOQUEIOS_DE_CONECTOR"));
        }
        if (migracao.canCutOver() && "BLOCKED".equalsIgnoreCase(integracao.prontidaoShadow())) {
            findings.add(finding("CUTOVER_SEM_SHADOW", "Cutover liberado sem shadow pronto", "MIGRACAO", "CRITICAL", true, "O processo apareceu apto para corte, mas o shadow mode continua bloqueado.", "SINCRONIZAR_SHADOW_ANTES_DO_CORTE"));
        }
    }

    private void scanOperacao(ProcessoOperacaoAggregate operacao, List<ProcessoEncaixeFinding> findings) {
        if (!"READY".equalsIgnoreCase(operacao.readiness())) {
            findings.add(finding("OPERACAO_NAO_PRONTA", "Operação ainda não pronta", "OPERACAO", "ATTENTION", false, String.join(" | ", operacao.alertas().stream().limit(3).toList()), "REDUZIR_SATURACAO_OPERACIONAL"));
        }
        if (operacao.totalBloqueios() > 0 && operacao.saturacaoMaxima() >= 60d) {
            findings.add(finding("SATURACAO_OPERACIONAL", "Saturação operacional elevada", "OPERACAO", "ATTENTION", false, "A operação do processo excedeu a faixa ideal de saturação.", "REDISTRIBUIR_CARGA_E_GATES"));
        }
    }

    private Processo load(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));
    }

    private ProcessoEncaixeFinding finding(String codigo,
                                           String titulo,
                                           String eixo,
                                           String severidade,
                                           boolean bloqueante,
                                           String detalhe,
                                           String remediacao) {
        return new ProcessoEncaixeFinding(codigo, titulo, eixo, severidade, bloqueante, detalhe, remediacao);
    }

    private int severityRank(ProcessoEncaixeFinding finding) {
        return switch (finding.severidade().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 0;
            case "ATTENTION" -> 1;
            case "INFO" -> 2;
            default -> 3;
        };
    }

    private long score(List<ProcessoEncaixeFinding> findings, ProcessoOperacaoAggregate operacao) {
        long critical = findings.stream().filter(item -> "CRITICAL".equals(item.severidade())).count();
        long attention = findings.stream().filter(item -> "ATTENTION".equals(item.severidade())).count();
        long base = 100L - critical * 12L - attention * 5L - Math.round(operacao.saturacaoMaxima() / 20d);
        return Math.max(0L, Math.min(100L, base));
    }

    private String readiness(long score, long bloqueantes) {
        if (bloqueantes > 0 || score < 60L) {
            return "NAO_PRONTO";
        }
        if (score < 85L) {
            return "PRONTO_COM_RESTRICOES";
        }
        return "PRONTO";
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }

    private boolean containsAny(String value, String... probes) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (String probe : probes) {
            if (normalized.contains(probe.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static final class FindingAccumulator {
        private final ProcessoEncaixeFinding finding;
        private long count;

        private FindingAccumulator(ProcessoEncaixeFinding finding) {
            this.finding = finding;
            this.count = 0L;
        }

        private void increment() {
            count++;
        }

        private ProcessoEncaixeFinding finding() {
            return finding;
        }

        private long count() {
            return count;
        }
    }
}
