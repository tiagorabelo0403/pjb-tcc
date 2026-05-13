package com.tcc.pjb.backend.service.publico;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.dto.publico.AtoTimelineDto;
import com.tcc.pjb.backend.model.dto.publico.TimelinePublicaDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class PublicProcessoTimelineService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final WorkItemRepository workItemRepository;

    public PublicProcessoTimelineService(ProcessoRepository processoRepository,
                                        MovimentacaoProcessualRepository movimentacaoRepository,
                                        WorkItemRepository workItemRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "public_timeline", key = "#numero")
    public TimelinePublicaDto timeline(String numero) {
        Processo processo = processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));
        NivelSigilo sigilo = processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;
        if (sigilo.exigeCredencial()) {
            throw new RecursoNaoEncontradoException("Processo", numero);
        }

        List<AtoTimelineDto> atos = movimentacaoRepository.findTop80ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId())
                .stream()
                .sorted(Comparator.comparing(MovimentacaoProcessual::getDataMovimentacao))
                .map(this::toAto)
                .toList();
        AtoTimelineDto proximoPasso = buildNextStep(processo);
        return new TimelinePublicaDto(
                processo.getNumeroProcesso(),
                buildResumo(processo, atos),
                atos,
                proximoPasso
        );
    }

    private AtoTimelineDto toAto(MovimentacaoProcessual movimentacao) {
        Instant data = movimentacao.getDataMovimentacao();
        AtoTimelineDto.StatusAto status = AtoTimelineDto.StatusAto.CONCLUIDO;
        return new AtoTimelineDto(
                simplificar(movimentacao.getDescricao(), movimentacao),
                data,
                status,
                movimentacao.getAtor() != null ? movimentacao.getAtor().getNome() : "Sistema judicial",
                null
        );
    }

    private AtoTimelineDto buildNextStep(Processo processo) {
        Instant dueAt = workItemRepository.minOpenDueAtForProcesso(processo.getId());
        if (dueAt != null) {
            AtoTimelineDto.StatusAto status = dueAt.isBefore(Instant.now())
                    ? AtoTimelineDto.StatusAto.ATRASADO
                    : AtoTimelineDto.StatusAto.PENDENTE;
            return new AtoTimelineDto(
                    "Próximo passo previsto em fila interna do processo",
                    dueAt,
                    status,
                    processo.getVara(),
                    null
            );
        }
        Instant referencia = processo.getDataUltimaMovimentacao() != null
                ? processo.getDataUltimaMovimentacao().atZone(java.time.ZoneId.systemDefault()).toInstant()
                : Instant.now();
        return new AtoTimelineDto(
                "Aguardar próxima movimentação oficial do processo",
                referencia.plus(15, ChronoUnit.DAYS),
                AtoTimelineDto.StatusAto.PENDENTE,
                processo.getVara(),
                null
        );
    }

    private String buildResumo(Processo processo, List<AtoTimelineDto> atos) {
        StringBuilder builder = new StringBuilder();
        builder.append("Processo ")
                .append(processo.getClasseProcessual() != null ? processo.getClasseProcessual() : "judicial")
                .append(" sobre ")
                .append(processo.getAssunto() != null ? processo.getAssunto() : "matéria não informada")
                .append(".");
        if (!atos.isEmpty()) {
            builder.append(' ')
                    .append("Último marco público: ")
                    .append(atos.getLast().descricaoSimples())
                    .append('.');
        }
        return builder.toString();
    }

    private String simplificar(String descricao, MovimentacaoProcessual movimentacao) {
        String base = descricao == null || descricao.isBlank()
                ? (movimentacao.getFasePara() != null ? movimentacao.getFasePara().name() : "MOVIMENTACAO_REGISTRADA")
                : descricao.trim();
        String normalized = base.toUpperCase(Locale.ROOT);
        if (normalized.contains("CITAC")) return "A outra parte foi chamada oficialmente para participar do processo";
        if (normalized.contains("INTIMA")) return "Foi enviada comunicação oficial sobre o andamento do processo";
        if (normalized.contains("AUDI")) return "Foi marcada ou realizada audiência no processo";
        if (normalized.contains("PERIC")) return "Houve andamento relacionado à prova técnica ou perícia";
        if (normalized.contains("SENTEN")) return "O juiz proferiu sentença no processo";
        if (normalized.contains("ACORDAO")) return "O tribunal publicou decisão colegiada no processo";
        if (normalized.contains("RECURS")) return "Foi registrado recurso para revisão da decisão";
        return Character.toUpperCase(base.charAt(0)) + base.substring(1).toLowerCase(Locale.ROOT);
    }
}
