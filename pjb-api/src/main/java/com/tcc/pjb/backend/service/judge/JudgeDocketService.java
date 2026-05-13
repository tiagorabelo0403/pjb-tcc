package com.tcc.pjb.backend.service.judge;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.judge.JudgeDocketItemDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.util.Locale;

@Service
public class JudgeDocketService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbTimeService timeService;

    public JudgeDocketService(CurrentUserService currentUserService, ProcessoRepository processoRepository, WorkItemRepository workItemRepository, PjbTimeService timeService) {
        this.currentUserService = currentUserService;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.timeService = timeService;
    }

    
    public String cacheUserKey() {
        Usuario u = currentUserService.getRequired();
        return u.getId() != null ? String.valueOf(u.getId()) : "anon";
    }

    @Cacheable(
            cacheNames = "judge_docket",
            key = "#limit + ':' + #root.target.cacheUserKey()",
            condition = "@cacheRuntime.redisEnabled()"
    )
    public List<JudgeDocketItemDto> docket(int limit) {
        Usuario u = currentUserService.getRequired();
        TipoUsuario tipo = u.getTipoUsuario();
        if (tipo == null || !tipo.isMagistratura()) {
            throw new IllegalStateException("Acesso restrito à magistratura.");
        }

        int hardLimit = Math.max(1, Math.min(limit, 200));

        
        var page = processoRepository.findForMagistradoDashboard(
                normalize(u.getUf()),
                normalize(u.getComarca()),
                PageRequest.of(0, Math.max(hardLimit, 120), Sort.by(Sort.Direction.DESC, "dataUltimaMovimentacao"))
        );

        List<JudgeDocketItemDto> mapped = new ArrayList<>();
        for (Processo p : page.getContent()) {
            ScoreResult s = score(p);
            mapped.add(toDto(p, s.score, s.reasons));
        }

        
        mapped.sort(Comparator
                .comparingInt(JudgeDocketItemDto::getUrgencyScore).reversed()
                .thenComparing(JudgeDocketItemDto::getUltimaMovimentacao, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        if (mapped.size() > hardLimit) {
            return mapped.subList(0, hardLimit);
        }
        return mapped;
    }

    private JudgeDocketItemDto toDto(Processo p, int score, List<String> reasons) {
        String jurisdicao = null;
        if (p.getJurisdicao() != null) {
            jurisdicao = p.getJurisdicao().getSigla() + " - " + p.getJurisdicao().getNome();
        }
        return JudgeDocketItemDto.builder()
                .processoId(p.getId())
                .numero(p.getNumeroUnificado())
                .classeProcessual(p.getClasseProcessual())
                .assunto(p.getAssunto())
                .jurisdicao(jurisdicao)
                .status(p.getStatusProcesso())
                .fase(p.getFaseAtual())
                .ultimaMovimentacao(p.getDataUltimaMovimentacao())
                .urgencyScore(score)
                .reasons(reasons)
                .build();
    }

    private record ScoreResult(int score, List<String> reasons) {}

    
    private ScoreResult score(Processo p) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        
        if (isSaude(p)) {
            score += 40;
            reasons.add("saúde/medicamento/internação");
        }

        
        if (p.getRamoDireito() == RamoDireito.INFANCIA_JUVENTUDE) {
            score += 25;
            reasons.add("infância/juventude");
        }

        
        if (p.getRamoDireito() == RamoDireito.PENAL) {
            score += 20;
            reasons.add("penal");
        }

        
        
        long blockingJudge = workItemRepository.countOpenBlockingByProcessAndRole(p.getId(), TipoUsuario.JUIZ);
        if (blockingJudge > 0) {
            score += 18;
            reasons.add("checklist do juiz pendente");
        }
        Instant minDue = workItemRepository.minDueAtOpen(p.getId());
        if (minDue != null) {
            Instant now = timeService.nowUtc();
            long hours = Duration.between(now, minDue).toHours();
            if (hours <= 24) {
                score += 10;
                reasons.add("SLA até 24h");
            } else if (hours <= 72) {
                score += 5;
                reasons.add("SLA até 72h");
            }
        }


        if (p.getStatusProcesso() != null) {
            switch (p.getStatusProcesso()) {
                case AGUARDANDO_PARECER -> {
                    score += 12;
                    reasons.add("aguardando parecer");
                }
                case PROTOCOLADO -> {
                    score += 6;
                    reasons.add("recém protocolado");
                }
                case EM_ANDAMENTO -> {
                    score += 4;
                    reasons.add("em andamento");
                }
                default -> {
                }
            }
        }

        
        LocalDateTime last = p.getDataUltimaMovimentacao();
        if (last != null) {
            long days = Duration.between(last, LocalDateTime.now()).toDays();
            if (days >= 90) {
                score += 20;
                reasons.add("+90 dias sem movimentação");
            } else if (days >= 30) {
                score += 10;
                reasons.add("+30 dias sem movimentação");
            }
        }

        
        if (p.getScoreComplexidade() != null && p.getScoreComplexidade() >= 80) {
            score += 6;
            reasons.add("alta complexidade");
        }

        
        score = Math.min(100, score);
        return new ScoreResult(score, reasons);
    }

    private boolean isSaude(Processo p) {
        String hay = String.join(" ",
                Optional.ofNullable(p.getModulo()).map(Enum::name).orElse(""),
                Optional.ofNullable(p.getAssunto()).orElse(""),
                Optional.ofNullable(p.getClasseProcessual()).orElse(""),
                Optional.ofNullable(p.getResumoIA()).orElse("")
        ).toUpperCase(Locale.ROOT);

        
        String[] keys = {
                "SAUDE", "MEDICAMENTO", "REMEDIO", "TRATAMENTO", "CIRURGIA", "UTI",
                "INTERNACAO", "HOME CARE", "ONCO", "CANCER", "HEMODIALISE",
                "TUTELA", "LIMINAR", "RISCO DE MORTE"
        };
        for (String k : keys) {
            if (hay.contains(k)) return true;
        }
        return false;
    }

    private String normalize(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }
}
