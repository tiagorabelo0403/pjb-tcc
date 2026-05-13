package com.tcc.pjb.backend.core.processo.unificado.application;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoFinding;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class ProcessoUnificadoApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final NationalProcessRoutingService routingService;

    public ProcessoUnificadoApplicationService(ProcessoRepository processoRepository,
                                               ProcessoLifecycleMachine lifecycleMachine,
                                               NationalProcessRoutingService routingService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.lifecycleMachine = Objects.requireNonNull(lifecycleMachine);
        this.routingService = Objects.requireNonNull(routingService);
    }

    public ProcessoUnificadoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        ProcessoUnificadoCompetencia competencia = competencia(processoId);
        List<ProcessoUnificadoAto> catalogo = catalogoAtos(processoId);
        List<ProcessoUnificadoAto> permitidos = catalogo.stream().filter(ProcessoUnificadoAto::permitido).toList();
        List<ProcessoUnificadoAto> bloqueados = catalogo.stream().filter(ato -> !ato.permitido()).toList();
        ProcessoUnificadoDiagnostico diagnostico = diagnosticar(processoId);
        return new ProcessoUnificadoAggregate(
                identidade(processo),
                competencia,
                diagnostico,
                permitidos,
                bloqueados,
                proximoMelhorAto(permitidos),
                Instant.now()
        );
    }

    public ProcessoUnificadoCompetencia competencia(Long processoId) {
        Processo processo = loadProcesso(processoId);
        GrauJurisdicao grau = inferirGrau(processo);
        NationalProcessRoutingService.RoutingDecision decision = routingService.route(new NationalProcessRoutingService.RoutingCommand(
                processo.getRito(),
                processo.getRamoDireito(),
                grau,
                processo.getUf(),
                processo.getComarca(),
                processo.getValorCausa() == null ? BigDecimal.ZERO : processo.getValorCausa(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                Instant.now(),
                processo.getNumeroProcesso(),
                processo.getComarca(),
                processo.getVara(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                processo.getTribunal(),
                hasLinkageMode(processo),
                hasLinkageMode(processo),
                hasLinkageMode(processo),
                isUrgente(processo),
                false,
                processo.getNivelSigilo() != null && processo.getNivelSigilo().name().contains("SIGILO"),
                false
        ));
        return new ProcessoUnificadoCompetencia(
                decision.tipoJustica().name(),
                decision.grau().name(),
                decision.ramoDireito().name(),
                decision.rito().name(),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                decision.tribunalCodigo(),
                decision.tribunalNome(),
                decision.orgaoJulgadorSugerido(),
                decision.unidadeJudiciariaCodigo(),
                decision.filaDistribuicao(),
                decision.mesaTriagem(),
                decision.territorialMode(),
                decision.preventionMode(),
                decision.distributionMode(),
                decision.specializationAxis(),
                decision.allocationStrategy(),
                decision.linkageMode(),
                decision.competenceEnvelope(),
                decision.routingRiskLevel(),
                decision.suggestedDeskProfile(),
                decision.sigiloPadrao(),
                decision.conciliacaoObrigatoria(),
                decision.prazoTriagemHoras(),
                decision.alertas(),
                decision.fundamentos(),
                decision.reviewChecklist(),
                decision.metadata()
        );
    }

    public List<ProcessoUnificadoAto> catalogoAtos(Long processoId) {
        Processo processo = loadProcesso(processoId);
        return Stream.of(ProcessoLifecycleAction.values())
                .map(action -> lifecycleMachine.preview(processo, action))
                .map(this::toAto)
                .sorted(Comparator
                        .comparing(ProcessoUnificadoAto::permitido).reversed()
                        .thenComparing(ProcessoUnificadoAto::sensivel).reversed()
                        .thenComparing(ProcessoUnificadoAto::recursal).reversed()
                        .thenComparing(ProcessoUnificadoAto::titulo))
                .toList();
    }

    public ProcessoUnificadoDiagnostico diagnosticar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        ProcessoUnificadoCompetencia competencia = competencia(processoId);
        List<ProcessoUnificadoAto> atos = catalogoAtos(processoId);
        ArrayList<ProcessoUnificadoFinding> findings = new ArrayList<>();
        if (processo.getRito() == null) {
            findings.add(new ProcessoUnificadoFinding("PROCESSO_SEM_RITO", "HIGH", true, "Processo sem rito processual", "O rito processual é obrigatório para coerência, distribuição e cabimento de atos."));
        }
        if (processo.getRamoDireito() == null) {
            findings.add(new ProcessoUnificadoFinding("PROCESSO_SEM_RAMO", "HIGH", true, "Processo sem ramo do direito", "O ramo do direito é obrigatório para competência material e trilha processual."));
        }
        if (processo.getClasseProcessual() == null || processo.getClasseProcessual().isBlank()) {
            findings.add(new ProcessoUnificadoFinding("PROCESSO_SEM_CLASSE", "MEDIUM", false, "Classe processual ausente", "A classe processual precisa ser consolidada para roteamento e filtros operacionais."));
        }
        if (processo.getAssunto() == null || processo.getAssunto().isBlank()) {
            findings.add(new ProcessoUnificadoFinding("PROCESSO_SEM_ASSUNTO", "LOW", false, "Assunto ausente", "O assunto processual reforça especialização, urgência e sugestão do próximo ato."));
        }
        if (processo.getTribunal() == null || processo.getTribunal().isBlank()) {
            findings.add(new ProcessoUnificadoFinding("PROCESSO_SEM_TRIBUNAL", "MEDIUM", false, "Tribunal não consolidado", "O tribunal roteado deve ser materializado para rastreabilidade e distribuição."));
        }
        if (processo.getVara() == null || processo.getVara().isBlank()) {
            findings.add(new ProcessoUnificadoFinding("PROCESSO_SEM_UNIDADE", "MEDIUM", false, "Unidade judiciária ausente", "A unidade judiciária sustenta a fila operacional e a lotação do processo."));
        }
        if (competencia.reviewChecklist().isEmpty()) {
            findings.add(new ProcessoUnificadoFinding("COMPETENCIA_SEM_CHECKLIST", "MEDIUM", false, "Roteamento sem checklist", "O envelope de competência deve produzir checklist de revisão antes da distribuição e redistribuição."));
        }
        long atosPermitidos = atos.stream().filter(ProcessoUnificadoAto::permitido).count();
        if (atosPermitidos == 0) {
            findings.add(new ProcessoUnificadoFinding("SEM_ATOS_PERMITIDOS", "HIGH", true, "Nenhum ato permitido", "O estado atual do processo não admite qualquer ato no ciclo mapeado."));
        }
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isArquivadoOuBaixado()) {
            boolean soReaberturaOuArquivo = atos.stream()
                    .filter(ProcessoUnificadoAto::permitido)
                    .allMatch(ato -> ato.codigo().equals("DESARQUIVAR") || ato.codigo().equals("ARQUIVAR"));
            if (!soReaberturaOuArquivo) {
                findings.add(new ProcessoUnificadoFinding("ATO_INCOMPATIVEL_COM_ARQUIVO", "HIGH", true, "Ato incompatível com processo encerrado", "Processo arquivado ou baixado só deve admitir reativação ou fechamento formal compatível."));
            }
        }
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("A coerência do processo nasce da união entre rito, ramo, fase, status, competência e ato cabível.");
        fundamentos.add("Distribuição e cabimento não devem se separar do mesmo envelope de domínio.");
        fundamentos.add("O processo unificado precisa preservar uma linha única entre catálogo de atos, motor de competência e ciclo de vida.");
        return new ProcessoUnificadoDiagnostico(
                findings.stream().noneMatch(ProcessoUnificadoFinding::blocking),
                findings.size(),
                findings.stream().filter(ProcessoUnificadoFinding::blocking).count(),
                atosPermitidos,
                atos.stream().filter(ato -> !ato.permitido()).count(),
                atos.stream().filter(ProcessoUnificadoAto::sensivel).count(),
                atos.stream().filter(ProcessoUnificadoAto::exigeSegurancaElevada).count(),
                findings.stream().sorted(Comparator.comparing(ProcessoUnificadoFinding::severity).reversed().thenComparing(ProcessoUnificadoFinding::code)).toList(),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private ProcessoUnificadoIdentity identidade(Processo processo) {
        LinkedHashSet<String> etiquetas = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) {
            etiquetas.add(processo.getRamoDireito().name());
        }
        if (processo.getRito() != null) {
            etiquetas.add(processo.getRito().name());
        }
        if (processo.getFaseAtual() != null) {
            etiquetas.add(processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            etiquetas.add(processo.getStatusProcesso().name());
        }
        if (isUrgente(processo)) {
            etiquetas.add("URGENTE");
        }
        if (processo.getNivelSigilo() != null) {
            etiquetas.add(processo.getNivelSigilo().name());
        }
        return new ProcessoUnificadoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getNumeroUnificado(),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                processo.getVara(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getParteAutoraNome(),
                processo.getParteReuNome(),
                List.copyOf(etiquetas)
        );
    }

    private ProcessoUnificadoAto toAto(ProcessoLifecycleDecision decision) {
        return new ProcessoUnificadoAto(
                decision.action().name(),
                decision.atoProcessual().titulo(),
                decision.atoProcessual().categoria().name(),
                decision.atoProcessual().workItemType().name(),
                decision.action().operationalAxis(),
                decision.atoProcessual().filaPadrao(),
                decision.atoProcessual().inboxPadrao(),
                decision.atoProcessual().fundamentoPadrao(),
                safeName(decision.faseOrigem()),
                safeName(decision.faseDestino()),
                safeName(decision.statusOrigem()),
                safeName(decision.statusDestino()),
                decision.permitida(),
                decision.isRecursalTransition(),
                decision.isTerminalTransition(),
                decision.action().isSensitiveJudicial(),
                decision.requiresMagistrature(),
                decision.atoProcessual().requiresElevatedSecurity(),
                decision.motivo(),
                decision.responsavelSugerido(),
                decision.transitionKey(),
                decision.alertas()
        );
    }

    private List<String> proximoMelhorAto(List<ProcessoUnificadoAto> permitidos) {
        return permitidos.stream()
                .sorted(Comparator
                        .comparing(ProcessoUnificadoAto::sensivel).reversed()
                        .thenComparing(ProcessoUnificadoAto::recursal).reversed()
                        .thenComparing(ProcessoUnificadoAto::terminal)
                        .thenComparing(ProcessoUnificadoAto::titulo))
                .limit(5)
                .map(ato -> ato.codigo() + ':' + ato.titulo())
                .toList();
    }

    private GrauJurisdicao inferirGrau(Processo processo) {
        String tribunal = normalize(processo.getTribunal());
        String unidade = normalize(processo.getVara());
        if (tribunal.startsWith("STF")) {
            return GrauJurisdicao.CONSTITUCIONAL;
        }
        if (tribunal.startsWith("STJ") || tribunal.startsWith("TST") || tribunal.startsWith("TSE") || tribunal.startsWith("STM")) {
            return GrauJurisdicao.SUPERIOR;
        }
        if (containsAny(unidade, "CAMARA", "TURMA", "SECAO", "COLEGIADO", "GABINETE_DESEMBARGADOR", "GABINETE_RELATOR")
                || containsAny(tribunal, "TJ", "TRF", "TRT", "TRE", "TJM") && processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            return GrauJurisdicao.SEGUNDO_GRAU;
        }
        return GrauJurisdicao.PRIMEIRO_GRAU;
    }

    private boolean hasLinkageMode(Processo processo) {
        return processo.getLinkageMode() != null && !processo.getLinkageMode().isBlank();
    }

    private boolean isUrgente(Processo processo) {
        return containsAny(normalize(processo.getAssunto()), "URG", "LIMINAR", "TUTELA", "CUSTODIA", "MEDIDA_PROTETIVA")
                || containsAny(normalize(processo.getClasseProcessual()), "HABEAS", "MANDADO_DE_SEGURANCA");
    }

    private boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank()) {
            return false;
        }
        for (String token : tokens) {
            if (source.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }
}
