package com.tcc.pjb.backend.service.intelligence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoConsultaResumo;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoGovernanceMetricas;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoCanalConsulta;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoConsultaGovernada;
import com.tcc.pjb.backend.model.repository.intelligence.PessoaLocalizacaoConsultaGovernadaRepository;

@Service
public class PessoaLocalizacaoIntelligenceSummaryService {

    private final PessoaLocalizacaoConsultaGovernadaRepository repository;
    private final PjbTimeService timeService;

    public PessoaLocalizacaoIntelligenceSummaryService(PessoaLocalizacaoConsultaGovernadaRepository repository,
                                                       PjbTimeService timeService) {
        this.repository = Objects.requireNonNull(repository);
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Transactional(readOnly = true)
    public PessoaLocalizacaoGovernanceMetricas resumir(Usuario executor,
                                                       PessoaLocalizacaoService.CanalConsulta canal,
                                                       int recentLimit) {
        if (executor == null || executor.getId() == null || canal == null) {
            return new PessoaLocalizacaoGovernanceMetricas(canal, null, null, 0, 0, 0, 0, 0, 0, 0d, "SEM_DADOS", false, List.of(), List.of());
        }
        int safeLimit = Math.max(1, Math.min(recentLimit, 20));
        Long executorId = executor.getId();
        Instant now = timeService.nowUtc();
        LocalDateTime start24h = LocalDateTime.ofInstant(now.minusSeconds(24 * 3600L), timeService.legalZone());
        LocalDateTime start7d = LocalDateTime.ofInstant(now.minusSeconds(7 * 24 * 3600L), timeService.legalZone());

        long ultimas24h = repository.countByExecutorUserIdAndCanalConsultaAndCreatedAtGreaterThanEqual(executorId, toEntityCanal(canal), start24h);
        long ultimos7d = repository.countByExecutorUserIdAndCanalConsultaAndCreatedAtGreaterThanEqual(executorId, toEntityCanal(canal), start7d);
        long revisao = repository.countByExecutorUserIdAndCanalConsultaAndRequerRevisaoTrueAndCreatedAtGreaterThanEqual(executorId, toEntityCanal(canal), start7d);
        long enderecoEstrito = repository.countByExecutorUserIdAndCanalConsultaAndEnderecoEstritoLiberadoTrueAndCreatedAtGreaterThanEqual(executorId, toEntityCanal(canal), start7d);
        long semContexto = repository.countByExecutorUserIdAndCanalConsultaAndPossuiContextoFormalFalseAndCreatedAtGreaterThanEqual(executorId, toEntityCanal(canal), start7d);
        long stepUpPendentes = repository.countByExecutorUserIdAndCanalConsultaAndStepUpRequiredTrueAndStepUpSatisfiedFalseAndCreatedAtGreaterThanEqual(executorId, toEntityCanal(canal), start7d);

        List<PessoaLocalizacaoConsultaGovernada> amostraPostural = repository.findByExecutorUserIdAndCanalConsultaOrderByCreatedAtDesc(
                executorId,
                toEntityCanal(canal),
                PageRequest.of(0, 200)
        );

        int totalScores = 0;
        Map<String, Integer> posturas = new LinkedHashMap<>();
        for (PessoaLocalizacaoConsultaGovernada entity : amostraPostural) {
            if (entity == null) {
                continue;
            }
            totalScores += entity.getPosturaScore();
            String postura = entity.getPosturaNivel() == null || entity.getPosturaNivel().isBlank() ? "DESCONHECIDA" : entity.getPosturaNivel().trim().toUpperCase();
            posturas.merge(postura, 1, Integer::sum);
        }

        double scoreMedio = amostraPostural.isEmpty() ? 0d : ((double) totalScores / (double) amostraPostural.size());
        scoreMedio = Math.round(scoreMedio * 100.0d) / 100.0d;
        String posturaPredominante = posturas.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
                })
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("SEM_DADOS");
        boolean exigeAtencao = revisao > 0 || stepUpPendentes > 0 || "ALTO".equals(posturaPredominante) || "CRITICO".equals(posturaPredominante);

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (revisao > 0) {
            alertas.add("Há consultas governadas exigindo revisão.");
        }
        if (stepUpPendentes > 0) {
            alertas.add("Há consultas sensíveis com step-up pendente.");
        }
        if (semContexto > 0) {
            alertas.add("Existem consultas sem contexto formal no histórico recente.");
        }
        if (amostraPostural.isEmpty()) {
            alertas.add("Sem consultas governadas registradas para o canal.");
        }

        List<PessoaLocalizacaoConsultaResumo> recentes = repository.findByExecutorUserIdAndCanalConsultaOrderByCreatedAtDesc(
                        executorId,
                        toEntityCanal(canal),
                        PageRequest.of(0, safeLimit)
                ).stream()
                .map(this::toResumo)
                .toList();

        return new PessoaLocalizacaoGovernanceMetricas(
                canal,
                start24h.atZone(timeService.legalZone()).toInstant(),
                start7d.atZone(timeService.legalZone()).toInstant(),
                safeLongToInt(ultimas24h),
                safeLongToInt(ultimos7d),
                safeLongToInt(revisao),
                safeLongToInt(enderecoEstrito),
                safeLongToInt(semContexto),
                safeLongToInt(stepUpPendentes),
                scoreMedio,
                posturaPredominante,
                exigeAtencao,
                recentes,
                List.copyOf(alertas)
        );
    }
    private static PessoaLocalizacaoCanalConsulta toEntityCanal(PessoaLocalizacaoService.CanalConsulta canal) {
        return canal == null ? null : PessoaLocalizacaoCanalConsulta.valueOf(canal.name());
    }

    private static PessoaLocalizacaoService.CanalConsulta fromEntityCanal(PessoaLocalizacaoCanalConsulta canal) {
        return canal == null ? null : PessoaLocalizacaoService.CanalConsulta.valueOf(canal.name());
    }

    private PessoaLocalizacaoConsultaResumo toResumo(PessoaLocalizacaoConsultaGovernada entity) {
        return new PessoaLocalizacaoConsultaResumo(
                entity.getCorrelationId(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().atZone(timeService.legalZone()).toInstant(),
                fromEntityCanal(entity.getCanalConsulta()),
                entity.getFundamento(),
                entity.getReferenciaProcedimental(),
                entity.getFinalidade(),
                entity.getNivelExposicao(),
                entity.getPosturaNivel(),
                entity.getPosturaScore(),
                entity.isRequerRevisao(),
                entity.isPossuiContextoFormal(),
                entity.isEnderecoEstritoLiberado(),
                entity.getFontesConsultadas(),
                entity.getEnderecosEncontrados(),
                entity.getRestricoesEncontradas(),
                entity.getVinculosEncontrados()
        );
    }

    private static int safeLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
