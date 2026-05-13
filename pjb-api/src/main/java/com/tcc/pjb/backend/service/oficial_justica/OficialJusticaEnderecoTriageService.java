package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaEnderecoTriageResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPessoaRastreioResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.profile.DiligenceRouteOptimizationService;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaEnderecoTriageService {

    private static final List<String> DEFAULT_COLUMNS = List.of(
            "processoNumero",
            "workItemId",
            "targetPolo",
            "targetNome",
            "cpfMascarado",
            "prioridadeOperacional",
            "score",
            "melhorEndereco",
            "cidadeUf",
            "fonteEndereco",
            "confiancaEndereco",
            "receitaStatus",
            "processosAtivosPessoa",
            "prazoFatalEm",
            "recomendacao"
    );

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final WorkItemRepository workItemRepository;
    private final ProcessoRepository processoRepository;
    private final PessoaLocalizacaoService pessoaLocalizacaoService;
    private final IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService;
    private final ProntuarioNacionalService prontuarioNacionalService;
    private final DocumentoNacionalValidator documentoNacionalValidator;
    private final DiligenceRouteOptimizationService diligenceRouteOptimizationService;
    private final PjbTimeService timeService;

    public OficialJusticaEnderecoTriageService(PerfilDashboardContextFactory contextFactory,
                                               PainelServiceCommons commons,
                                               WorkItemRepository workItemRepository,
                                               ProcessoRepository processoRepository,
                                               PessoaLocalizacaoService pessoaLocalizacaoService,
                                               IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService,
                                               ProntuarioNacionalService prontuarioNacionalService,
                                               DocumentoNacionalValidator documentoNacionalValidator,
                                               DiligenceRouteOptimizationService diligenceRouteOptimizationService,
                                               PjbTimeService timeService) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.commons = Objects.requireNonNull(commons);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.pessoaLocalizacaoService = Objects.requireNonNull(pessoaLocalizacaoService);
        this.identidadeJuridicaNacionalService = Objects.requireNonNull(identidadeJuridicaNacionalService);
        this.prontuarioNacionalService = Objects.requireNonNull(prontuarioNacionalService);
        this.documentoNacionalValidator = Objects.requireNonNull(documentoNacionalValidator);
        this.diligenceRouteOptimizationService = Objects.requireNonNull(diligenceRouteOptimizationService);
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painelResumo() {
        Usuario usuario = contextFactory.build().usuario();
        List<WorkItem> mandados = commons.inboxHibrido(usuario, 40).stream()
                .filter(this::isMandadoOuDiligencia)
                .toList();
        long criticos = mandados.stream().filter(this::isCriticalByDueDate).count();
        long semCpf = mandados.stream().filter(item -> resolveTarget(item).cpf() == null).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "PLANILHA_TRIAGEM_OPERACIONAL");
        out.put("activationPath", "/api/v1/oficial-justica/localizador/triagem-enderecos");
        out.put("trackingByMandadoPath", "/api/v1/oficial-justica/localizador/mandados/{mandadoId}/rastreio");
        out.put("trackingByProcessPath", "/api/v1/oficial-justica/localizador/processos/{processoId}/alvos/{polo}/rastreio");
        out.put("totalMandadosElegiveis", mandados.size());
        out.put("criticosHojeOuAtrasados", criticos);
        out.put("semCpfEstruturado", semCpf);
        out.put("dependsOnProcessProgress", Boolean.TRUE);
        out.put("supportsReceitaSignals", Boolean.TRUE);
        out.put("supportsRouteSuggestion", Boolean.TRUE);
        out.put("columns", DEFAULT_COLUMNS);
        return Collections.unmodifiableMap(out);
    }

    @Transactional(readOnly = true)
    public OficialJusticaEnderecoTriageResponse triagem(int limit,
                                                        boolean incluirEnderecoEstrito,
                                                        boolean incluirProntuario,
                                                        boolean incluirRestricoes) {
        Usuario usuario = contextFactory.build().usuario();
        int safeLimit = Math.max(1, Math.min(limit, 12));
        List<WorkItem> mandados = commons.inboxHibrido(usuario, Math.max(24, safeLimit * 4)).stream()
                .filter(this::isMandadoOuDiligencia)
                .sorted(Comparator.comparing(this::urgencyRank).reversed())
                .limit(safeLimit)
                .toList();

        List<OficialJusticaEnderecoTriageResponse.TriageRow> rows = new ArrayList<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        for (WorkItem item : mandados) {
            try {
                rows.add(buildRow(item, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes));
            } catch (Exception ex) {
                alerts.add("Falha pontual ao materializar triagem do mandado " + item.getId() + ": " + ex.getClass().getSimpleName());
            }
        }
        rows = rows.stream()
                .sorted(Comparator.comparingInt(OficialJusticaEnderecoTriageResponse.TriageRow::score).reversed()
                        .thenComparing(OficialJusticaEnderecoTriageResponse.TriageRow::prazoFatalEm, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int criticos = (int) rows.stream().filter(row -> "CRITICA".equals(row.prioridadeOperacional())).count();
        int altas = (int) rows.stream().filter(row -> "ALTA".equals(row.prioridadeOperacional())).count();
        int comEndereco = (int) rows.stream().filter(row -> row.totalEnderecos() > 0).count();
        int semEndereco = rows.size() - comEndereco;
        int comReceita = (int) rows.stream().filter(row -> row.receitaStatus() != null && !row.receitaStatus().isBlank()).count();
        int comProntuarioAtivo = (int) rows.stream().filter(row -> row.processosAtivosPessoa() > 0).count();
        int pendenciaFatal = (int) rows.stream().filter(row -> row.prazoFatalEm() != null && !row.prazoFatalEm().isAfter(timeService.nowUtc())).count();
        if (rows.stream().allMatch(row -> row.totalEnderecos() == 0)) {
            alerts.add("Nenhum endereço materializado pelas fontes governadas nesta execução; usar rastreio por mandado para aprofundamento ou corrigir cadastro da parte.");
        }
        if (rows.stream().anyMatch(row -> row.receitaStatus() != null && "PENDENTE".equalsIgnoreCase(row.receitaStatus()))) {
            alerts.add("Há alvos com identidade ainda pendente na trilha Receita/Gov federada.");
        }
        DiligenceRouteOptimizationResponse rota = rows.isEmpty() ? null : buildRouteSuggestion(rows);
        return new OficialJusticaEnderecoTriageResponse(
                composeTerritorio(usuario),
                timeService.nowUtc(),
                new OficialJusticaEnderecoTriageResponse.Summary(rows.size(), criticos, altas, comEndereco, semEndereco, comReceita, comProntuarioAtivo, pendenciaFatal),
                DEFAULT_COLUMNS,
                rows,
                rota,
                List.copyOf(alerts)
        );
    }

    @Transactional(readOnly = true)
    public OficialJusticaPessoaRastreioResponse rastrearMandado(String mandadoId,
                                                                boolean incluirEnderecoEstrito,
                                                                boolean incluirProntuario,
                                                                boolean incluirRestricoes) {
        WorkItem item = resolveMandado(mandadoId);
        TrackingContext context = resolveTrackingContext(item.getProcesso(), item, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes);
        List<String> alerts = new ArrayList<>(context.alerts());
        if (context.target().cpf() == null) {
            alerts.add("Mandado sem CPF estruturado da parte alvo; corrigir o cadastro processual para obter rastreio governado completo.");
        }
        return new OficialJusticaPessoaRastreioResponse(
                "MANDADO",
                timeService.nowUtc(),
                item.getProcessoId(),
                item.getId(),
                item.getProcesso() != null ? item.getProcesso().getNumeroProcesso() : null,
                mandadoId,
                new OficialJusticaPessoaRastreioResponse.Target(
                        context.target().polo(),
                        context.target().nome(),
                        context.target().cpfMascarado(),
                        context.target().cpf() != null,
                        PessoaLocalizacaoFundamento.CUMPRIMENTO_MANDADO.name(),
                        context.recomendacao()
                ),
                buildProcessContext(item.getProcesso(), item),
                context.receitaSignals(),
                context.prontuarioSignals(),
                context.localizacao(),
                context.heuristicaOperacional(),
                List.copyOf(alerts)
        );
    }

    @Transactional(readOnly = true)
    public OficialJusticaPessoaRastreioResponse rastrearProcessoAlvo(Long processoId,
                                                                      String polo,
                                                                      boolean incluirEnderecoEstrito,
                                                                      boolean incluirProntuario,
                                                                      boolean incluirRestricoes) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Target target = resolveTarget(processo, polo);
        TrackingContext context = buildTrackingContext(processo, null, target, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes);
        List<String> alerts = new ArrayList<>(context.alerts());
        if (context.target().cpf() == null) {
            alerts.add("Processo sem CPF estruturado para o polo solicitado; corrigir cadastro da parte para rastreio governado.");
        }
        return new OficialJusticaPessoaRastreioResponse(
                "PROCESSO_POLO",
                timeService.nowUtc(),
                processoId,
                null,
                processo.getNumeroProcesso(),
                null,
                new OficialJusticaPessoaRastreioResponse.Target(
                        context.target().polo(),
                        context.target().nome(),
                        context.target().cpfMascarado(),
                        context.target().cpf() != null,
                        PessoaLocalizacaoFundamento.CUMPRIMENTO_MANDADO.name(),
                        context.recomendacao()
                ),
                buildProcessContext(processo, null),
                context.receitaSignals(),
                context.prontuarioSignals(),
                context.localizacao(),
                context.heuristicaOperacional(),
                List.copyOf(alerts)
        );
    }

    private OficialJusticaEnderecoTriageResponse.TriageRow buildRow(WorkItem item,
                                                                    boolean incluirEnderecoEstrito,
                                                                    boolean incluirProntuario,
                                                                    boolean incluirRestricoes) {
        TrackingContext context = resolveTrackingContext(item.getProcesso(), item, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes);
        PessoaLocalizacaoResponse.EnderecoCandidato melhorEndereco = selectBestAddress(context.localizacao());
        int score = score(item, context.localizacao(), context.prontuario(), context.receitaStatus());
        String prioridade = classifyScore(score);
        String cidadeUf = melhorEndereco == null ? fallbackCidadeUf(item.getProcesso(), item) : joinCidadeUf(melhorEndereco.cidade(), melhorEndereco.uf());
        return new OficialJusticaEnderecoTriageResponse.TriageRow(
                item.getId(),
                item.getProcessoId(),
                item.getProcesso() != null ? item.getProcesso().getNumeroProcesso() : null,
                context.target().polo(),
                context.target().nome(),
                context.target().cpfMascarado(),
                item.getProcesso() != null && item.getProcesso().getStatusProcesso() != null ? item.getProcesso().getStatusProcesso().name() : null,
                item.getProcesso() != null && item.getProcesso().getFaseAtual() != null ? item.getProcesso().getFaseAtual().name() : null,
                item.getDueAt(),
                prioridade,
                score,
                melhorEndereco == null ? fallbackAddress(item.getProcesso(), item) : melhorEndereco.descricao(),
                cidadeUf,
                melhorEndereco == null ? fallbackSource(item.getProcesso(), item) : melhorEndereco.fonte(),
                melhorEndereco == null ? null : melhorEndereco.confianca(),
                context.localizacao() != null && context.localizacao().enderecoEstritoLiberado(),
                context.localizacao() == null ? 0 : context.localizacao().enderecos().size(),
                context.receitaStatus(),
                context.prontuario() == null ? 0 : context.prontuario().processosAtivos(),
                context.prontuario() == null ? null : context.prontuario().prontuarioNacionalUri(),
                context.recomendacao(),
                context.alerts()
        );
    }

    private TrackingContext resolveTrackingContext(Processo processo,
                                                   WorkItem item,
                                                   boolean incluirEnderecoEstrito,
                                                   boolean incluirProntuario,
                                                   boolean incluirRestricoes) {
        return buildTrackingContext(processo, item, resolveTarget(item), incluirEnderecoEstrito, incluirProntuario, incluirRestricoes);
    }

    private TrackingContext buildTrackingContext(Processo processo,
                                                 WorkItem item,
                                                 Target target,
                                                 boolean incluirEnderecoEstrito,
                                                 boolean incluirProntuario,
                                                 boolean incluirRestricoes) {
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        if (target.cpf() == null) {
            return new TrackingContext(target, null, null, null, null, buildHeuristics(processo, item, null, null, null, "Corrigir CPF do alvo processual antes da diligência."), "Corrigir CPF do alvo processual antes da diligência.", List.copyOf(alerts));
        }
        IdentidadeJuridicaNacionalService.IdentidadeResumo identidadeResumo = identidadeJuridicaNacionalService.buscarPorDocumento(target.cpf())
                .map(identity -> IdentidadeJuridicaNacionalService.IdentidadeResumo.of(identity, documentoNacionalValidator.mascararDocumento(identity.getDocumento())))
                .orElse(null);
        if (identidadeResumo == null) {
            alerts.add("Identidade nacional ainda não sincronizada para o CPF consultado.");
        }
        ProntuarioNacionalService.ProntuarioNacionalView prontuario = incluirProntuario ? prontuarioNacionalService.consultarPorDocumento(target.cpf()) : null;
        if (prontuario != null && prontuario.processosAtivos() > 0) {
            alerts.add("Pessoa possui histórico processual ativo no prontuário nacional.");
        }
        PessoaLocalizacaoResponse localizacao = pessoaLocalizacaoService.localizar(new PessoaLocalizacaoRequest(
                target.cpf(),
                processo != null ? processo.getId() : null,
                item != null ? item.getId() : null,
                PessoaLocalizacaoFundamento.CUMPRIMENTO_MANDADO,
                "Rastreio operacional do oficial de justiça para diligência dependente do andamento processual.",
                "Consulta governada para planejamento de cumprimento, prevenção de diligência frustrada e priorização da rota diária.",
                buildReferenciaProcedimental(processo, item),
                incluirProntuario,
                incluirRestricoes,
                false,
                incluirEnderecoEstrito
        ), PessoaLocalizacaoService.CanalConsulta.OFICIAL_JUSTICA);
        if (localizacao.enderecos().isEmpty()) {
            alerts.add("Sem endereço materializado na consulta governada desta execução.");
        }
        String receitaStatus = identidadeResumo == null ? null : identidadeResumo.receitaStatus();
        String recomendacao = recommend(item, localizacao, prontuario, receitaStatus);
        return new TrackingContext(
                target,
                localizacao,
                identidadeResumo,
                prontuario,
                receitaSignals(identidadeResumo),
                buildHeuristics(processo, item, localizacao, prontuario, receitaStatus, recomendacao),
                recomendacao,
                List.copyOf(alerts)
        );
    }

    private Map<String, Object> buildHeuristics(Processo processo,
                                                WorkItem item,
                                                PessoaLocalizacaoResponse localizacao,
                                                ProntuarioNacionalService.ProntuarioNacionalView prontuario,
                                                String receitaStatus,
                                                String recomendacao) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("urgencyRank", item == null ? 0 : urgencyRank(item));
        out.put("scoreEstimado", item == null ? 0 : score(item, localizacao, prontuario, receitaStatus));
        out.put("dependsOnProcessProgress", Boolean.TRUE);
        out.put("processoAtivo", processo != null && processo.getStatusProcesso() != null && processo.getStatusProcesso() == StatusProcesso.EM_ANDAMENTO);
        out.put("enderecosEncontrados", localizacao == null ? 0 : localizacao.enderecos().size());
        out.put("prontuarioAtivos", prontuario == null ? 0 : prontuario.processosAtivos());
        out.put("recomendacao", recomendacao);
        return Map.copyOf(filterNulls(out));
    }

    private Map<String, Object> receitaSignals(IdentidadeJuridicaNacionalService.IdentidadeResumo resumo) {
        if (resumo == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("identidadeId", resumo.id());
        out.put("nomeCanonico", resumo.nomeCanonico());
        out.put("documentoMascarado", resumo.documentoMascarado());
        out.put("prontuarioUri", resumo.prontuarioNacionalUri());
        out.put("nivelConfianca", resumo.nivelConfianca());
        out.put("receitaStatus", resumo.receitaStatus());
        out.put("oabStatus", resumo.oabStatus());
        out.put("govBrVinculado", resumo.govBrVinculado());
        return Map.copyOf(filterNulls(out));
    }

    private Map<String, Object> prontuarioSignals(ProntuarioNacionalService.ProntuarioNacionalView prontuario) {
        if (prontuario == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("documentoMascarado", prontuario.documentoMascarado());
        out.put("nomeCanonico", prontuario.nomeCanonico());
        out.put("totalProcessos", prontuario.totalProcessos());
        out.put("processosAtivos", prontuario.processosAtivos());
        out.put("processosArquivados", prontuario.processosArquivados());
        out.put("tribunaisDistintos", prontuario.tribunaisDistintos());
        out.put("govBrVinculado", prontuario.govBrVinculado());
        out.put("prontuarioNacionalUri", prontuario.prontuarioNacionalUri());
        out.put("consultadoEm", prontuario.consultadoEm());
        return Map.copyOf(filterNulls(out));
    }

    private DiligenceRouteOptimizationResponse buildRouteSuggestion(List<OficialJusticaEnderecoTriageResponse.TriageRow> rows) {
        List<DiligenceRouteOptimizationRequest.StopInput> stops = rows.stream()
                .filter(row -> row.melhorEndereco() != null && !row.melhorEndereco().isBlank())
                .limit(8)
                .map(row -> new DiligenceRouteOptimizationRequest.StopInput(
                        row.workItemId() == null ? row.processoNumero() : String.valueOf(row.workItemId()),
                        row.processoNumero() == null ? row.targetNome() : row.processoNumero() + " - " + row.targetNome(),
                        row.melhorEndereco(),
                        null,
                        null,
                        routePriority(row.prioridadeOperacional()),
                        row.prazoFatalEm(),
                        null,
                        null
                ))
                .toList();
        if (stops.isEmpty()) {
            return null;
        }
        return diligenceRouteOptimizationService.optimize(new DiligenceRouteOptimizationRequest(null, null, 18, stops));
    }

    private int routePriority(String prioridadeOperacional) {
        return switch (normalize(prioridadeOperacional)) {
            case "CRITICA" -> 1;
            case "ALTA" -> 2;
            case "MEDIA" -> 3;
            default -> 4;
        };
    }

    private OficialJusticaPessoaRastreioResponse.ProcessoContext buildProcessContext(Processo processo, WorkItem item) {
        return new OficialJusticaPessoaRastreioResponse.ProcessoContext(
                processo != null && processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null,
                processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo != null ? processo.getClasseProcessual() : null,
                processo != null ? processo.getAssunto() : null,
                item != null ? item.getDueAt() : null,
                processo != null ? processo.getComarca() : null,
                processo != null ? processo.getUf() : null
        );
    }

    private PessoaLocalizacaoResponse.EnderecoCandidato selectBestAddress(PessoaLocalizacaoResponse response) {
        if (response == null || response.enderecos() == null || response.enderecos().isEmpty()) {
            return null;
        }
        return response.enderecos().stream()
                .sorted(Comparator.comparing(PessoaLocalizacaoResponse.EnderecoCandidato::principal).reversed()
                        .thenComparing(PessoaLocalizacaoResponse.EnderecoCandidato::parcial)
                        .thenComparing(PessoaLocalizacaoResponse.EnderecoCandidato::confianca, Comparator.reverseOrder())
                        .thenComparing(PessoaLocalizacaoResponse.EnderecoCandidato::atualizadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private String recommend(WorkItem item,
                             PessoaLocalizacaoResponse response,
                             ProntuarioNacionalService.ProntuarioNacionalView prontuario,
                             String receitaStatus) {
        boolean hasAddress = response != null && response.enderecos() != null && !response.enderecos().isEmpty();
        boolean overdue = item != null && item.getDueAt() != null && !item.getDueAt().isAfter(timeService.nowUtc());
        if (overdue && hasAddress) {
            return "Cumprimento imediato com endereço materializado e prioridade de fila crítica.";
        }
        if (hasAddress) {
            return "Planejar diligência na rota do dia e confirmar janela operacional antes da saída.";
        }
        if (prontuario != null && prontuario.processosAtivos() > 0) {
            return "Sem endereço atual, mas com sinais de trilha nacional ativa; aprofundar rastreio antes de diligência presencial.";
        }
        if (receitaStatus != null && !receitaStatus.isBlank()) {
            return "Usar sinais federados de identidade/Receita como trilha auxiliar e revisar cadastro processual.";
        }
        return "Dados insuficientes para diligência presencial segura; corrigir cadastro e reexecutar rastreio.";
    }

    private int score(WorkItem item,
                      PessoaLocalizacaoResponse response,
                      ProntuarioNacionalService.ProntuarioNacionalView prontuario,
                      String receitaStatus) {
        int score = 0;
        score += urgencyRank(item) * 8;
        if (item != null && item.getPrioridade() != null) {
            score += Math.max(0, 6 - item.getPrioridade()) * 3;
        }
        if (response != null && response.enderecos() != null && !response.enderecos().isEmpty()) {
            score += 18;
            PessoaLocalizacaoResponse.EnderecoCandidato top = selectBestAddress(response);
            if (top != null) {
                score += Math.max(0, Math.min(15, (int) Math.round(top.confianca() * 10d)));
                if (top.principal()) {
                    score += 6;
                }
                if (!top.parcial()) {
                    score += 4;
                }
            }
        } else {
            score -= 8;
        }
        if (prontuario != null) {
            score += Math.min(10, prontuario.processosAtivos());
        }
        if (receitaStatus != null && !receitaStatus.isBlank() && !"PENDENTE".equalsIgnoreCase(receitaStatus)) {
            score += 5;
        }
        if (item != null && commons.titleContains(item, "CITACAO", "INTIMACAO", "PENHORA", "BUSCA")) {
            score += 6;
        }
        return Math.max(0, score);
    }

    private String classifyScore(int score) {
        if (score >= 70) {
            return "CRITICA";
        }
        if (score >= 50) {
            return "ALTA";
        }
        if (score >= 30) {
            return "MEDIA";
        }
        return "TRIAGEM";
    }

    private int urgencyRank(WorkItem item) {
        if (item == null || item.getDueAt() == null) {
            return 1;
        }
        long hours = ChronoUnit.HOURS.between(timeService.nowUtc(), item.getDueAt());
        if (hours <= 0) {
            return 5;
        }
        if (hours <= 24) {
            return 4;
        }
        if (hours <= 72) {
            return 3;
        }
        if (hours <= 168) {
            return 2;
        }
        return 1;
    }

    private boolean isCriticalByDueDate(WorkItem item) {
        return urgencyRank(item) >= 4;
    }

    private boolean isMandadoOuDiligencia(WorkItem item) {
        return item != null && commons.titleContains(item, "MANDADO", "CITACAO", "INTIMACAO", "BUSCA", "PENHORA", "DILIGENCIA");
    }

    private Target resolveTarget(WorkItem item) {
        return resolveTarget(item == null ? null : item.getProcesso(), inferPolo(item));
    }

    private Target resolveTarget(Processo processo, String poloHint) {
        String normalized = normalize(poloHint);
        boolean ativoPreferencial = "ATIVO".equals(normalized) || "AUTOR".equals(normalized) || "REQUERENTE".equals(normalized) || "EXEQUENTE".equals(normalized);
        String cpf = ativoPreferencial ? firstNonBlank(processo != null ? processo.getParteAutoraCpf() : null, processo != null ? processo.getParteReuCpf() : null)
                : firstNonBlank(processo != null ? processo.getParteReuCpf() : null, processo != null ? processo.getParteAutoraCpf() : null);
        String nome = ativoPreferencial ? firstNonBlank(processo != null ? processo.getParteAutoraNome() : null, processo != null ? processo.getParteReuNome() : null)
                : firstNonBlank(processo != null ? processo.getParteReuNome() : null, processo != null ? processo.getParteAutoraNome() : null);
        String polo = ativoPreferencial ? "ATIVO" : "PASSIVO";
        String cpfNormalizado = cpf == null ? null : documentoNacionalValidator.normalizarDocumento(cpf);
        String cpfMascarado = cpfNormalizado == null || cpfNormalizado.isBlank() ? null : documentoNacionalValidator.mascararDocumento(cpfNormalizado);
        return new Target(polo, nome == null ? "ALVO_NAO_IDENTIFICADO" : nome, cpfNormalizado, cpfMascarado);
    }

    private String inferPolo(WorkItem item) {
        if (item == null) {
            return "PASSIVO";
        }
        String normalized = normalize((item.getTitulo() == null ? "" : item.getTitulo()) + " " + (item.getDescricao() == null ? "" : item.getDescricao()));
        if (normalized.contains("AUTOR") || normalized.contains("REQUERENTE") || normalized.contains("EXEQUENTE") || normalized.contains("ALIMENTANTE")) {
            return "ATIVO";
        }
        return "PASSIVO";
    }

    private String fallbackCidadeUf(Processo processo, WorkItem item) {
        return joinCidadeUf(
                processo != null ? processo.getComarca() : item != null ? item.getComarca() : null,
                processo != null ? processo.getUf() : item != null ? item.getUf() : null
        );
    }

    private String fallbackAddress(Processo processo, WorkItem item) {
        String cidadeUf = fallbackCidadeUf(processo, item);
        return cidadeUf == null ? "ENDERECO_NAO_MATERIALIZADO" : "Referência territorial do processo: " + cidadeUf;
    }

    private String fallbackSource(Processo processo, WorkItem item) {
        return processo != null ? "PROCESSO_TERRITORIAL" : item != null ? "MANDADO_TERRITORIAL" : "SEM_FONTE";
    }

    private String buildReferenciaProcedimental(Processo processo, WorkItem item) {
        return firstNonBlank(
                processo != null ? processo.getNumeroProcesso() : null,
                item != null && item.getId() != null ? "MANDADO:" + item.getId() : null,
                "OFICIAL_JUSTICA_RASTREIO"
        );
    }

    private WorkItem resolveMandado(String mandadoId) {
        try {
            Long id = Long.parseLong(mandadoId);
            return workItemRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("WorkItem", id));
        } catch (NumberFormatException ex) {
            throw new RecursoNaoEncontradoException("WorkItem", mandadoId);
        }
    }

    private static String joinCidadeUf(String cidade, String uf) {
        String cidadeNormalizada = firstNonBlank(cidade);
        String ufNormalizada = firstNonBlank(uf);
        if (cidadeNormalizada == null && ufNormalizada == null) {
            return null;
        }
        if (cidadeNormalizada == null) {
            return ufNormalizada;
        }
        if (ufNormalizada == null) {
            return cidadeNormalizada;
        }
        return cidadeNormalizada + "/" + ufNormalizada;
    }

    private String composeTerritorio(Usuario usuario) {
        if (usuario == null) {
            return "NAO_INFORMADO";
        }
        return firstNonBlank(joinCidadeUf(usuario.getComarca(), usuario.getUf()), usuario.getUf(), "NAO_INFORMADO");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
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

    private static Map<String, Object> filterNulls(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null) {
            return out;
        }
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out;
    }

    private record Target(String polo, String nome, String cpf, String cpfMascarado) {
    }

    private record TrackingContext(Target target,
                                   PessoaLocalizacaoResponse localizacao,
                                   IdentidadeJuridicaNacionalService.IdentidadeResumo identidadeResumo,
                                   ProntuarioNacionalService.ProntuarioNacionalView prontuario,
                                   Map<String, Object> receitaSignals,
                                   Map<String, Object> heuristicaOperacional,
                                   String recomendacao,
                                   List<String> alerts) {
        private String receitaStatus() {
            return identidadeResumo == null ? null : identidadeResumo.receitaStatus();
        }

        private Map<String, Object> prontuarioSignals() {
            if (prontuario == null) {
                return Map.of();
            }
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("documentoMascarado", prontuario.documentoMascarado());
            out.put("nomeCanonico", prontuario.nomeCanonico());
            out.put("totalProcessos", prontuario.totalProcessos());
            out.put("processosAtivos", prontuario.processosAtivos());
            out.put("processosArquivados", prontuario.processosArquivados());
            out.put("tribunaisDistintos", prontuario.tribunaisDistintos());
            out.put("govBrVinculado", prontuario.govBrVinculado());
            out.put("prontuarioNacionalUri", prontuario.prontuarioNacionalUri());
            out.put("consultadoEm", prontuario.consultadoEm());
            return Map.copyOf(filterNulls(out));
        }
    }
}
