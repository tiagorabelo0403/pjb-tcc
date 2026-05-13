package com.tcc.pjb.backend.service.oficial_justica;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaOperationalIntelligenceResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;

@Service
public class OficialJusticaOperationalIntelligenceService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final WorkItemRepository workItemRepository;
    private final PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService;
    private final OficialJusticaEnderecoTriageService enderecoTriageService;

    public OficialJusticaOperationalIntelligenceService(PerfilDashboardContextFactory contextFactory,
                                                        PainelServiceCommons commons,
                                                        WorkItemRepository workItemRepository,
                                                        PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService,
                                                        OficialJusticaEnderecoTriageService enderecoTriageService) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.commons = Objects.requireNonNull(commons);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.intelligenceSummaryService = Objects.requireNonNull(intelligenceSummaryService);
        this.enderecoTriageService = Objects.requireNonNull(enderecoTriageService);
    }

    public OficialJusticaOperationalIntelligenceResponse analyze(int horizonDays, long filaCriticaThreshold) {
        Usuario usuario = contextFactory.build().usuario();
        Instant now = Instant.now();
        Instant horizon = now.plus(Math.max(1, horizonDays), ChronoUnit.DAYS);
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 100).stream()
                .filter(this::isMandadoOuDiligencia)
                .toList();
        int pendentes = inbox.size();
        int vencendo = (int) inbox.stream().filter(item -> item.getDueAt() != null && !item.getDueAt().isBefore(now) && !item.getDueAt().isAfter(horizon)).count();
        int atrasados = (int) inbox.stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(now)).count();
        int citacoes = (int) inbox.stream().filter(item -> contains(item, "CITACAO")).count();
        int intimacoes = (int) inbox.stream().filter(item -> contains(item, "INTIMACAO")).count();
        int penhoras = (int) inbox.stream().filter(item -> contains(item, "PENHORA", "AVALIACAO")).count();
        List<String> filasCriticas = workItemRepository.findFilasComMaisDe(Math.max(1L, filaCriticaThreshold));
        List<OficialJusticaOperationalIntelligenceResponse.DiligenciaView> proximas = inbox.stream()
                .sorted(java.util.Comparator.comparing(WorkItem::getDueAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .limit(12)
                .map(item -> new OficialJusticaOperationalIntelligenceResponse.DiligenciaView(
                        item.getId(),
                        item.getProcesso() != null ? item.getProcesso().getNumeroProcesso() : null,
                        item.getTitulo(),
                        item.getComarca(),
                        item.getDueAt() != null ? item.getDueAt().toString() : null,
                        item.getType() != null ? item.getType().name() : null
                ))
                .toList();
        LinkedHashMap<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("inteligenciaLocalizacao", intelligenceSummaryService.resumir(usuario, PessoaLocalizacaoService.CanalConsulta.OFICIAL_JUSTICA, 6));
        metricas.put("capacidadeRota", proximas.size());
        metricas.put("rastreioOperacional", enderecoTriageService.painelResumo());
        metricas.put("janelaDias", Math.max(1, horizonDays));
        metricas.put("filaCriticaThreshold", Math.max(1L, filaCriticaThreshold));
        String territorio = composeTerritorio(usuario.getUf(), usuario.getComarca());
        return new OficialJusticaOperationalIntelligenceResponse(
                territorio,
                pendentes,
                vencendo,
                atrasados,
                citacoes,
                intimacoes,
                penhoras,
                filasCriticas,
                proximas,
                metricas
        );
    }

    private String composeTerritorio(String uf, String comarca) {
        String normalizedUf = normalizeNullable(uf);
        String normalizedComarca = normalizeNullable(comarca);
        if (normalizedUf == null && normalizedComarca == null) {
            return "NAO_INFORMADO";
        }
        if (normalizedUf == null) {
            return normalizedComarca;
        }
        if (normalizedComarca == null) {
            return normalizedUf;
        }
        return normalizedUf + ":" + normalizedComarca;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private boolean isMandadoOuDiligencia(WorkItem item) {
        return contains(item, "MANDADO", "CITACAO", "INTIMACAO", "BUSCA", "PENHORA", "DILIGENCIA");
    }

    private boolean contains(WorkItem item, String... tokens) {
        String haystack = (item.getTitulo() == null ? "" : item.getTitulo())
                + ' ' + (item.getDescricao() == null ? "" : item.getDescricao())
                + ' ' + (item.getType() == null ? "" : item.getType().name());
        String normalized = java.text.Normalizer.normalize(haystack, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(java.util.Locale.ROOT);
        for (String token : tokens) {
            String probe = java.text.Normalizer.normalize(token, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toUpperCase(java.util.Locale.ROOT);
            if (normalized.contains(probe)) {
                return true;
            }
        }
        return false;
    }
}
