package com.tcc.pjb.backend.service.profile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.BehavioralAuditResumo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.security.PerfilBehaviorBaselineRepository;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;

@Service
public class PerfilBehavioralAuditService {

    private final WorkItemRepository workItemRepository;
    private final ProcessoRepository processoRepository;
    private final PeritoNomeacaoRepository peritoNomeacaoRepository;
    private final AuditoriaInteligenteService auditoriaInteligenteService;
    private final PerfilBehaviorBaselineRepository baselineRepository;
    private final PjbTimeService timeService;

    private static final Map<TipoUsuario, Integer> DEFAULT_BASELINE_MAX = Map.ofEntries(
            Map.entry(TipoUsuario.PERITO, 25),
            Map.entry(TipoUsuario.PERITO_MEDICO, 25),
            Map.entry(TipoUsuario.PSICOLOGO_JUDICIAL, 18),
            Map.entry(TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL, 18),
            Map.entry(TipoUsuario.OFICIAL_JUSTICA, 80),
            Map.entry(TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, 90),
            Map.entry(TipoUsuario.DELEGADO_POLICIA, 120),
            Map.entry(TipoUsuario.DELEGADO_POLICIA_FEDERAL, 140),
            Map.entry(TipoUsuario.MINISTRO, 400),
            Map.entry(TipoUsuario.ASSESSOR_JUDICIAL, 160),
            Map.entry(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, 120),
            Map.entry(TipoUsuario.LEILOEIRO_JUDICIAL, 40),
            Map.entry(TipoUsuario.TABELIAO, 110)
    );

    public PerfilBehavioralAuditService(WorkItemRepository workItemRepository,
                                        ProcessoRepository processoRepository,
                                        PeritoNomeacaoRepository peritoNomeacaoRepository,
                                        AuditoriaInteligenteService auditoriaInteligenteService,
                                        PerfilBehaviorBaselineRepository baselineRepository,
                                        PjbTimeService timeService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.peritoNomeacaoRepository = Objects.requireNonNull(peritoNomeacaoRepository);
        this.auditoriaInteligenteService = Objects.requireNonNull(auditoriaInteligenteService);
        this.baselineRepository = Objects.requireNonNull(baselineRepository);
        this.timeService = Objects.requireNonNull(timeService);
    }

    public BehavioralAuditResumo avaliar(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null || usuario.getId() == null) {
            return new BehavioralAuditResumo("UNKNOWN", 0, 0, "DESCONHECIDO", false, "Perfil não identificado.");
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        Baseline baseline = resolveBaseline(tipo);
        int observedVolume = medirVolume(tipo, usuario);
        double ratio = baseline.expectedMax() <= 0 ? 0d : (double) observedVolume / (double) baseline.expectedMax();
        String level = ratio >= baseline.anomalyThresholdRatio() ? "ANOMALO" : ratio >= baseline.alertThresholdRatio() ? "ALERTA" : "NORMAL";
        boolean anomalous = ratio >= baseline.anomalyThresholdRatio();
        String rationale = baseline.describe(observedVolume, ratio);
        if (anomalous) {
            try {
                auditoriaInteligenteService.registrarEventoImutavel("PERFIL_COMPORTAMENTO_ANOMALO", usuario.getId(), rationale + " @" + timeService.nowUtc());
            } catch (Exception ignored) {
            }
        }
        return new BehavioralAuditResumo(tipo.name(), observedVolume, baseline.expectedMax(), level, anomalous, rationale);
    }

    private int medirVolume(TipoUsuario tipo, Usuario usuario) {
        if (tipo.isPerito()) {
            return (int) peritoNomeacaoRepository.countByPerito_IdAndStatusIn(usuario.getId(), List.of(PeritoNomeacaoStatus.NOMEADO, PeritoNomeacaoStatus.ACEITO))
                    + (int) workItemRepository.countDueByAssignedUser(usuario.getId(), timeService.nowUtc().plusSeconds(7 * 86400L));
        }
        if (tipo.isMagistratura()) {
            return (int) processoRepository.findForMagistradoDashboard(usuario.getUf(), usuario.getComarca(), PageRequest.of(0, 1)).getTotalElements();
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            return (int) workItemRepository.countDueByAssignedUser(usuario.getId(), timeService.nowUtc().plusSeconds(3 * 86400L));
        }
        return (int) workItemRepository.countDueByAssignedUser(usuario.getId(), timeService.nowUtc().plusSeconds(7 * 86400L));
    }

    private Baseline resolveBaseline(TipoUsuario tipo) {
        var persisted = baselineRepository.findByTipoUsuarioAndAtivoTrue(tipo).orElse(null);
        if (persisted != null) {
            return new Baseline(
                    firstPositive(persisted.getExpectedVolume(), DEFAULT_BASELINE_MAX.getOrDefault(tipo, 90)),
                    firstPositive(persisted.getAlertThresholdRatio(), 1.10d),
                    Math.max(firstPositive(persisted.getAnomalyThresholdRatio(), 1.50d), firstPositive(persisted.getAlertThresholdRatio(), 1.10d)),
                    persisted.getRationale(),
                    "PERSISTIDO"
            );
        }
        return new Baseline(DEFAULT_BASELINE_MAX.getOrDefault(tipo, 90), 1.10d, 1.50d, null, "DEFAULT");
    }

    private static int firstPositive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private static double firstPositive(Double value, double fallback) {
        return value != null && value > 0d ? value : fallback;
    }

    private record Baseline(int expectedMax, double alertThresholdRatio, double anomalyThresholdRatio, String rationale, String source) {
        String describe(int observedVolume, double ratio) {
            String reason = rationale == null || rationale.isBlank() ? "baseline=" + source : rationale;
            return reason + " | volume_observado=" + observedVolume + " | limite=" + expectedMax + " | ratio=" + Math.round(ratio * 100.0d) / 100.0d;
        }
    }
}
