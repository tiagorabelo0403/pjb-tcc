package com.tcc.pjb.backend.service.publico;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.model.dto.publico.AtoTimelineDto;
import com.tcc.pjb.backend.model.dto.publico.TimelinePublicaDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.profile.LegalPlainLanguageService;
import com.tcc.pjb.backend.service.prazo.CongestionScoreService;

@Service
public class ProcessoGemeoDigitalPublicoService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final LegalPlainLanguageService legalPlainLanguageService;
    private final CongestionScoreService congestionScoreService;

    public ProcessoGemeoDigitalPublicoService(ProcessoRepository processoRepository,
                                              MovimentacaoProcessualRepository movimentacaoRepository,
                                              LegalPlainLanguageService legalPlainLanguageService,
                                              CongestionScoreService congestionScoreService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.legalPlainLanguageService = Objects.requireNonNull(legalPlainLanguageService);
        this.congestionScoreService = Objects.requireNonNull(congestionScoreService);
    }

    @Transactional(readOnly = true)
    public TimelinePublicaDto consultar(String numeroProcesso) {
        Processo processo = processoRepository.findByNumeroUnificado(numeroProcesso)
                .or(() -> processoRepository.findByNumeroProcesso(numeroProcesso))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "processo nao encontrado"));

        NivelSigilo sigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        if (sigilo.exigeCredencial()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "processo nao encontrado");
        }

        List<MovimentacaoProcessual> historico = movimentacaoRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId())
                .stream()
                .sorted(Comparator.comparing(MovimentacaoProcessual::getDataMovimentacao, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        ArrayList<AtoTimelineDto> atos = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < historico.size(); i++) {
            MovimentacaoProcessual item = historico.get(i);
            boolean ultimo = i == historico.size() - 1;
            atos.add(new AtoTimelineDto(
                    legalPlainLanguageService.translatePublicText(descricaoBase(item)),
                    item.getDataMovimentacao(),
                    statusOf(item, ultimo, now),
                    item.getAtor() != null ? item.getAtor().getNome() : inferResponsavel(item),
                    null
            ));
        }

        AtoTimelineDto proximoPasso = inferirProximoPasso(processo, atos);
        String descricaoSimples = legalPlainLanguageService.translatePublicText(buildResumo(processo, atos, proximoPasso));
        return new TimelinePublicaDto(resolveNumero(processo), descricaoSimples, List.copyOf(atos), proximoPasso);
    }

    private AtoTimelineDto inferirProximoPasso(Processo processo, List<AtoTimelineDto> atos) {
        String tipoAto = processo.getFaseAtual() != null ? processo.getFaseAtual().name() : "ATO_PROCESSUAL";
        var prediction = congestionScoreService.predizerPublicoPorNumero(resolveNumero(processo), tipoAto);
        String dataEstimada = prediction.dataEstimada() != null ? prediction.dataEstimada().toString() : "sem data estimada";
        String descricao = "Próximo passo provável: " + legalPlainLanguageService.translatePublicText(tipoAto)
                + " com estimativa em " + dataEstimada
                + " e congestion score " + String.format(java.util.Locale.ROOT, "%.2f", prediction.congestionScore()) + '.';
        AtoTimelineDto.StatusAto status = prediction.desvioPercentual() > 0.20d ? AtoTimelineDto.StatusAto.ATRASADO : AtoTimelineDto.StatusAto.PENDENTE;
        Instant when = prediction.dataEstimada() != null ? prediction.dataEstimada().atStartOfDay(ZoneId.systemDefault()).toInstant() : Instant.now();
        return new AtoTimelineDto(descricao, when, status, inferResponsavel(processo), null);
    }

    private AtoTimelineDto.StatusAto statusOf(MovimentacaoProcessual item, boolean ultimo, Instant now) {
        if (!ultimo) {
            return AtoTimelineDto.StatusAto.CONCLUIDO;
        }
        if (item.getDataMovimentacao() == null) {
            return AtoTimelineDto.StatusAto.PENDENTE;
        }
        Instant when = item.getDataMovimentacao();
        if (when.plusSeconds(60L * 60L * 24L * 20L).isBefore(now)) {
            return AtoTimelineDto.StatusAto.ATRASADO;
        }
        return AtoTimelineDto.StatusAto.PENDENTE;
    }

    private String descricaoBase(MovimentacaoProcessual item) {
        StringBuilder sb = new StringBuilder();
        if (item.getDescricao() != null && !item.getDescricao().isBlank()) {
            sb.append(item.getDescricao());
        }
        if (item.getFaseDe() != null || item.getFasePara() != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("Fase ")
                    .append(item.getFaseDe() != null ? item.getFaseDe().name() : "INICIAL")
                    .append(" para ")
                    .append(item.getFasePara() != null ? item.getFasePara().name() : "ATUAL");
        }
        return sb.length() == 0 ? "Movimentação processual registrada." : sb.toString();
    }

    private String buildResumo(Processo processo, List<AtoTimelineDto> atos, AtoTimelineDto proximoPasso) {
        StringBuilder sb = new StringBuilder();
        sb.append("Processo ").append(resolveNumero(processo)).append(". ");
        if (processo.getAssunto() != null && !processo.getAssunto().isBlank()) {
            sb.append("Assunto principal: ").append(processo.getAssunto()).append(". ");
        }
        if (!atos.isEmpty()) {
            sb.append("Último marco público: ").append(atos.get(atos.size() - 1).descricaoSimples()).append(". ");
        }
        if (proximoPasso != null) {
            sb.append(proximoPasso.descricaoSimples());
        }
        return sb.toString();
    }

    private String resolveNumero(Processo processo) {
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        return processo.getNumeroProcesso();
    }

    private String inferResponsavel(MovimentacaoProcessual item) {
        if (item.getFasePara() != null) {
            return item.getFasePara().name();
        }
        return "unidade judiciária";
    }

    private String inferResponsavel(Processo processo) {
        if (processo.getComarca() != null && !processo.getComarca().isBlank()) {
            return processo.getComarca();
        }
        return "unidade judiciária";
    }
}
