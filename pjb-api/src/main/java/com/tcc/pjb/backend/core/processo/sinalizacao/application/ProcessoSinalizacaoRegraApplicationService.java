package com.tcc.pjb.backend.core.processo.sinalizacao.application;

import com.tcc.pjb.backend.core.processo.pregravacao.application.ProcessoPreGravacaoApplicationService;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sinalizacao.domain.ProcessoSinalizacaoAggregate;
import com.tcc.pjb.backend.core.processo.sinalizacao.domain.ProcessoSinalizacaoSeparador;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoSinalizacaoRegraApplicationService {

    private static final String DEFAULT_PROFILE = "MAGISTRATURA__MAGISTRADO_TITULAR";

    private final ProcessoRepository processoRepository;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoSigiloApplicationService processoSigiloApplicationService;
    private final ProcessoPreGravacaoApplicationService processoPreGravacaoApplicationService;
    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    public ProcessoSinalizacaoRegraApplicationService(ProcessoRepository processoRepository,
                                                      ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                      ProcessoSigiloApplicationService processoSigiloApplicationService,
                                                      ProcessoPreGravacaoApplicationService processoPreGravacaoApplicationService,
                                                      ProcessoUnificadoApplicationService processoUnificadoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoSigiloApplicationService = Objects.requireNonNull(processoSigiloApplicationService);
        this.processoPreGravacaoApplicationService = Objects.requireNonNull(processoPreGravacaoApplicationService);
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
    }

    public ProcessoSinalizacaoAggregate detalhar(Long processoId, String profileCode) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoSigiloAggregate sigilo = processoSigiloApplicationService.detalhar(processoId);
        ProcessoUnificadoAggregate unificado = processoUnificadoApplicationService.detalhar(processoId);
        String acaoReferencia = unificado.atosPermitidos().stream().findFirst().map(ProcessoUnificadoAto::codigo).orElse("VISUALIZAR_PROCESSO");
        ProcessoPreGravacaoAggregate preGravacao = processoPreGravacaoApplicationService.avaliar(processoId, blank(profileCode) ? DEFAULT_PROFILE : profileCode, acaoReferencia);

        String accentColor = resolveAccentColor(processo, sigilo, timeline, preGravacao, unificado);
        String highlightColor = resolveHighlightColor(sigilo, timeline, preGravacao);
        String priorityBand = resolvePriorityBand(sigilo, timeline, preGravacao, processo);

        ArrayList<ProcessoSinalizacaoSeparador> separadores = new ArrayList<>();
        int ordem = 10;
        separadores.add(new ProcessoSinalizacaoSeparador("ESTADO_PROCESSUAL", tituloEstado(processo), ordem, "Estado central do processo guia a cor primária.", false));
        ordem += 10;
        if (sigilo.nivelSigilo().getNivel() > 0) {
            separadores.add(new ProcessoSinalizacaoSeparador("SIGILO", "Sigilo " + sigilo.nivelSigilo().name(), ordem, "Sigilo altera a camada visual e o acesso ao conteúdo.", sigilo.totalFindings() > 0));
            ordem += 10;
        }
        if (timeline.totalBloqueantes() > 0) {
            separadores.add(new ProcessoSinalizacaoSeparador("TRAVA_FLUXO", "Fluxo com trava ativa", ordem, "Há pendência bloqueante impedindo o próximo passo esperado.", true));
            ordem += 10;
        }
        if (preGravacao.blockingTriggers() > 0 || preGravacao.stepUpTriggers() > 0) {
            separadores.add(new ProcessoSinalizacaoSeparador("PRE_GRAVACAO_SENSIVEL", "Persistência sensível controlada", ordem, "O sistema exige guarda forte antes da gravação definitiva.", true));
            ordem += 10;
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            separadores.add(new ProcessoSinalizacaoSeparador("FASE_RECURSAL", "Faixa recursal", ordem, "A fase recursal precisa se destacar por tempestividade e cabimento.", false));
            ordem += 10;
        }
        if (containsExecution(processo)) {
            separadores.add(new ProcessoSinalizacaoSeparador("FASE_EXECUTIVA", "Faixa executiva", ordem, "Execução exige marcação visual própria por constrição e satisfação.", false));
            ordem += 10;
        }
        if (!timeline.proximoCiclo().isEmpty()) {
            separadores.add(new ProcessoSinalizacaoSeparador("PROXIMO_ATO", timeline.proximoCiclo().getFirst(), ordem, "O próximo ato esperado passa a guiar a priorização visual.", false));
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(sigilo.fundamentos());
        fundamentos.addAll(preGravacao.fundamentos());
        fundamentos.addAll(unificado.proximoMelhorAto());
        fundamentos.add("Catálogo nacional de cores e separadores agora deriva do estado processual vivo, não apenas do blueprint institucional.");

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (timeline.totalBloqueantes() > 0) {
            alertas.add("A sinalização visual foi elevada porque o fluxo possui bloqueio ativo.");
        }
        if (sigilo.nivelSigilo().getNivel() >= 2) {
            alertas.add("O processo exige leitura visual restritiva por sigilo reforçado.");
        }
        if (preGravacao.stepUpTriggers() > 0) {
            alertas.add("Step-up obrigatório antes de qualquer persistência sensível.");
        }
        return new ProcessoSinalizacaoAggregate(
                processo.getId(),
                processo.getNumeroProcesso(),
                accentColor,
                highlightColor,
                priorityBand,
                List.copyOf(separadores),
                List.copyOf(fundamentos),
                List.copyOf(alertas),
                Instant.now()
        );
    }

    private String resolveAccentColor(Processo processo,
                                      ProcessoSigiloAggregate sigilo,
                                      ProcessoTimelineAggregate timeline,
                                      ProcessoPreGravacaoAggregate preGravacao,
                                      ProcessoUnificadoAggregate unificado) {
        if (sigilo.nivelSigilo().getNivel() >= 5) return "black";
        if (sigilo.nivelSigilo().getNivel() >= 3) return "purple";
        if (preGravacao.blockingTriggers() > 0) return "red";
        if (timeline.totalBloqueantes() > 0) return "amber";
        if (isUrgente(processo, unificado)) return "rose";
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) return "amber";
        if (containsExecution(processo)) return "violet";
        return "slate";
    }

    private String resolveHighlightColor(ProcessoSigiloAggregate sigilo,
                                         ProcessoTimelineAggregate timeline,
                                         ProcessoPreGravacaoAggregate preGravacao) {
        if (sigilo.nivelSigilo().getNivel() >= 1) return "lock";
        if (preGravacao.stepUpTriggers() > 0) return "orange";
        if (timeline.totalBloqueantes() > 0) return "yellow";
        return "blue";
    }

    private String resolvePriorityBand(ProcessoSigiloAggregate sigilo,
                                       ProcessoTimelineAggregate timeline,
                                       ProcessoPreGravacaoAggregate preGravacao,
                                       Processo processo) {
        if (sigilo.nivelSigilo().getNivel() >= 4) return "ULTRA_RESTRITA";
        if (preGravacao.blockingTriggers() > 0 || timeline.totalBloqueantes() > 0) return "CRITICA";
        if (containsExecution(processo) || processo.getFaseAtual() == FaseProcessual.RECURSAL) return "ELEVADA";
        return "NORMAL";
    }

    private boolean isUrgente(Processo processo, ProcessoUnificadoAggregate unificado) {
        return containsAny(processo.getClasseProcessual(), "TUTELA", "CAUTELAR")
                || containsAny(processo.getAssunto(), "URG", "LIMINAR", "PLANTAO")
                || unificado.proximoMelhorAto().stream().map(this::normalize).anyMatch(item -> item.contains("URG") || item.contains("CUSTODIA"));
    }

    private boolean containsExecution(Processo processo) {
        return containsAny(processo.getRito() == null ? null : processo.getRito().name(), "EXECUCAO", "EXECUÇÃO")
                || containsAny(processo.getFaseAtual() == null ? null : processo.getFaseAtual().name(), "EXECUCAO", "CUMPRIMENTO");
    }

    private boolean containsAny(String raw, String... tokens) {
        String normalized = normalize(raw);
        for (String token : tokens) {
            if (normalized.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private String tituloEstado(Processo processo) {
        return String.join(" • ", List.of(
                processo.getRamoDireito() == null ? "RAMO" : processo.getRamoDireito().name(),
                processo.getFaseAtual() == null ? "FASE" : processo.getFaseAtual().name(),
                processo.getStatusProcesso() == null ? "STATUS" : processo.getStatusProcesso().name()
        ));
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
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
