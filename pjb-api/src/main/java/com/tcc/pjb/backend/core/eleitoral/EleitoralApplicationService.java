package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioConsultaCommand;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioConsultaResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioHealthView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioWindowAuditView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioWindowView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCargoView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralConsultaFeitoCommand;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralConsultaFeitoResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralDiplomacaoHealthSnapshot;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralDiplomacaoQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralDiplomacaoResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralDiplomacaoSyncSummary;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralDiplomacaoWindowAuditView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralDiplomacaoWindowView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralFeitoConsistencyView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralFeitoOwnershipView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralFeitoTimelineQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralFeitoTimelineResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralHealthQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralHealthResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPartidoView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPendenteDiplomacaoQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPendenteDiplomacaoResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPrestacaoContasConsistencyView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPrestacaoContasHealthSnapshot;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPrestacaoContasQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPrestacaoContasStatusView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPrestacaoContasView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPrestacaoContasWindowView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralResultadoWindowQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralResultadoWindowView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralSilencioQuery;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralSilencioView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralTimelineAuditSnapshot;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralZonaHealthResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralZonaHealthView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralZonaProcessoView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralZonaResult;
import com.tcc.pjb.backend.core.eleitoral.domain.FeitoEleitoralStatusSnapshot;
import com.tcc.pjb.backend.core.eleitoral.domain.SincronizarPrestacaoContasResult;
import com.tcc.pjb.backend.model.entity.eleitoral.ProcessoZonaEleitoral;
import com.tcc.pjb.backend.model.repository.ProcessoZonaEleitoralRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleitoralApplicationService {

    private final FeitoEleitoralService feitoEleitoralService;
    private final ProcessoZonaEleitoralRepository processoZonaEleitoralRepository;
    private final FeitoEleitoralDiplomacaoScheduler diplomacaoScheduler;
    private final EleitoralTseProperties properties;
    private final AuditLedgerService auditLedgerService;

    public EleitoralApplicationService(FeitoEleitoralService feitoEleitoralService,
                                       ProcessoZonaEleitoralRepository processoZonaEleitoralRepository,
                                       FeitoEleitoralDiplomacaoScheduler diplomacaoScheduler,
                                       EleitoralTseProperties properties,
                                       AuditLedgerService auditLedgerService) {
        this.feitoEleitoralService = Objects.requireNonNull(feitoEleitoralService);
        this.processoZonaEleitoralRepository = Objects.requireNonNull(processoZonaEleitoralRepository);
        this.diplomacaoScheduler = Objects.requireNonNull(diplomacaoScheduler);
        this.properties = Objects.requireNonNull(properties);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public EleitoralConsultaFeitoResult feito(Long processoId) {
        return feitoEleitoralService.consultarFeito(new EleitoralConsultaFeitoCommand(requireProcessoId(processoId)));
    }

    @Transactional(readOnly = true)
    public FeitoEleitoralStatusSnapshot status(Long processoId) {
        return feitoEleitoralService.statusSnapshot(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralHealthResult health(Long processoId, String uf, LocalDate dataConsulta) {
        return feitoEleitoralService.health(new EleitoralHealthQuery(requireProcessoId(processoId), normalizeUf(uf), safeDate(dataConsulta)));
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoTimelineResult timeline(Long processoId) {
        Long requiredId = requireProcessoId(processoId);
        EleitoralFeitoTimelineResult result = feitoEleitoralService.timeline(new EleitoralFeitoTimelineQuery(requiredId));
        auditLedgerService.appendSafely("ELEITORAL_TIMELINE_QUERY", "PROCESSO", String.valueOf(requiredId), null, "entries=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public EleitoralTimelineAuditSnapshot timelineAudit(Long processoId) {
        return feitoEleitoralService.timelineAudit(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoResult diplomacao(Long processoId) {
        return feitoEleitoralService.diplomacao(new EleitoralDiplomacaoQuery(requireProcessoId(processoId)));
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoHealthSnapshot diplomacaoHealth(Long processoId) {
        return feitoEleitoralService.diplomacaoHealth(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoWindowView diplomacaoWindow(Long processoId) {
        return feitoEleitoralService.diplomacaoWindowView(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoWindowAuditView diplomacaoWindowAudit(Long processoId) {
        return feitoEleitoralService.diplomacaoWindowAudit(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasView prestacaoContas(Long processoId) {
        return feitoEleitoralService.prestacaoContas(new EleitoralPrestacaoContasQuery(requireProcessoId(processoId)));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasStatusView prestacaoContasStatus(Long processoId) {
        return feitoEleitoralService.prestacaoContasStatus(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasHealthSnapshot prestacaoContasHealth(Long processoId) {
        return feitoEleitoralService.prestacaoContasHealth(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasConsistencyView prestacaoContasConsistency(Long processoId) {
        return feitoEleitoralService.prestacaoContasConsistencyView(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasWindowView prestacaoContasWindow(Long processoId) {
        return feitoEleitoralService.prestacaoContasWindowView(requireProcessoId(processoId));
    }

    @Transactional
    public SincronizarPrestacaoContasResult sincronizarPrestacaoContas(Long processoId) {
        Long requiredId = requireProcessoId(processoId);
        SincronizarPrestacaoContasResult result = feitoEleitoralService.sincronizarPrestacaoContasResult(requiredId);
        auditLedgerService.appendSafely("ELEITORAL_PRESTACAO_CONTAS_SYNC_MANUAL", "PROCESSO", String.valueOf(requiredId), null, "status=" + result.status());
        return result;
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoOwnershipView ownership(Long processoId) {
        return feitoEleitoralService.ownershipView(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoConsistencyView consistency(Long processoId) {
        return feitoEleitoralService.feitoConsistencyView(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralCargoView cargo(Long processoId) {
        return feitoEleitoralService.cargoView(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralPartidoView partido(Long processoId) {
        return feitoEleitoralService.partidoView(requireProcessoId(processoId));
    }

    @Transactional(readOnly = true)
    public EleitoralZonaResult zona(Long processoId) {
        ProcessoZonaEleitoral zona = loadZona(requireProcessoId(processoId));
        return new EleitoralZonaResult(zona.getProcessoId(), zona.getZonaEleitoral(), zona.getMunicipio(), zona.getUf(), zona.getCartorioCodigo());
    }

    @Transactional(readOnly = true)
    public EleitoralZonaProcessoView zonaProcesso(Long processoId) {
        ProcessoZonaEleitoral zona = loadZona(requireProcessoId(processoId));
        return new EleitoralZonaProcessoView(zona.getProcessoId(), zona.getZonaEleitoral(), zona.getMunicipio(), zona.getUf());
    }

    @Transactional(readOnly = true)
    public EleitoralZonaHealthView zonaHealth(Long processoId) {
        ProcessoZonaEleitoral zona = loadZona(requireProcessoId(processoId));
        String status = isBlank(zona.getZonaEleitoral()) || isBlank(zona.getUf()) ? "INCOMPLETO" : "OK";
        String detalhe = isBlank(zona.getCartorioCodigo()) ? "cartorio pendente" : "cartorio=" + zona.getCartorioCodigo();
        return new EleitoralZonaHealthView(String.valueOf(zona.getProcessoId()), status, detalhe);
    }

    @Transactional(readOnly = true)
    public EleitoralZonaHealthResult zonaHealthResult(Long processoId, String criterio) {
        Long requiredId = requireProcessoId(processoId);
        EleitoralZonaHealthView view = zonaHealth(requiredId);
        String effectiveCriterio = criterio == null || criterio.isBlank() ? "zona" : criterio.trim();
        auditLedgerService.appendSafely("ELEITORAL_ZONA_HEALTH_QUERY", "PROCESSO", String.valueOf(requiredId), null, "criterio=" + effectiveCriterio + " status=" + view.status());
        return new EleitoralZonaHealthResult("OK".equalsIgnoreCase(view.status()), view.detalhe(), Instant.now());
    }

    @Transactional(readOnly = true)
    public EleitoralPendenteDiplomacaoResult pendentes(Integer limit) {
        int effectiveLimit = limit == null || limit <= 0 ? 20 : limit;
        EleitoralPendenteDiplomacaoResult result = feitoEleitoralService.pendentes(new EleitoralPendenteDiplomacaoQuery(effectiveLimit));
        auditLedgerService.appendSafely("ELEITORAL_PENDENTES_DIPLOMACAO_QUERY", "ELEITORAL", "PENDENTES", null, "limit=" + effectiveLimit + " total=" + result.pendentes().size());
        return result;
    }

    @Transactional
    public EleitoralDiplomacaoSyncSummary diplomacaoSyncRun() {
        int pendentesAntes = feitoEleitoralService.listarFeitosPendentesDeDiplomacao().size();
        diplomacaoScheduler.sincronizarDiplomacoes();
        EleitoralDiplomacaoSyncSummary summary = new EleitoralDiplomacaoSyncSummary(properties.dryRun(), pendentesAntes, Instant.now());
        auditLedgerService.appendSafely("ELEITORAL_DIPLOMACAO_SYNC_RUN", "ELEITORAL", "DIPLOMACAO", null, "dryRun=" + summary.dryRun() + " pendentesAntes=" + summary.pendentesAntes());
        return summary;
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioConsultaResult calendario(String uf, LocalDate data) {
        return feitoEleitoralService.consultarCalendario(new EleitoralCalendarioConsultaCommand(normalizeUf(uf), safeDate(data)));
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioHealthView calendarioHealth(String uf, LocalDate data) {
        String normalizedUf = normalizeUf(uf);
        LocalDate effectiveDate = safeDate(data);
        EleitoralCalendarioHealthView view = feitoEleitoralService.calendarioHealth(new EleitoralCalendarioQuery(normalizedUf, effectiveDate));
        auditLedgerService.appendSafely("ELEITORAL_CALENDARIO_HEALTH_QUERY", "ELEITORAL_CALENDARIO", normalizedUf, null, "status=" + view.status());
        return view;
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioWindowView calendarioWindow(String uf, LocalDate data) {
        return feitoEleitoralService.calendarioWindowView(normalizeUf(uf), safeDate(data));
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioWindowAuditView calendarioWindowAudit(String uf, LocalDate data) {
        return feitoEleitoralService.calendarioWindowAudit(normalizeUf(uf), safeDate(data));
    }

    @Transactional(readOnly = true)
    public EleitoralResultadoWindowView resultadoWindow(String uf, String fase, LocalDate dataReferencia) {
        return feitoEleitoralService.resultadoWindowView(new EleitoralResultadoWindowQuery(normalizeUf(uf), safeDate(dataReferencia), normalizeReference(fase)));
    }

    @Transactional(readOnly = true)
    public EleitoralSilencioView silencio(String uf, LocalDate data, boolean tutelaUrgente) {
        return feitoEleitoralService.silencio(new EleitoralSilencioQuery(normalizeUf(uf), safeDate(data), tutelaUrgente));
    }

    private ProcessoZonaEleitoral loadZona(Long processoId) {
        return processoZonaEleitoralRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Zona eleitoral não encontrada para o processo: " + processoId));
    }

    private Long requireProcessoId(Long processoId) {
        if (processoId == null || processoId <= 0) {
            throw new IllegalArgumentException("processoId obrigatorio");
        }
        return processoId;
    }

    private LocalDate safeDate(LocalDate value) {
        return value == null ? LocalDate.now() : value;
    }

    private String normalizeUf(String uf) {
        String normalized = normalizeReference(uf);
        if (normalized.length() != 2) {
            throw new IllegalArgumentException("uf obrigatoria");
        }
        return normalized;
    }

    private String normalizeReference(String value) {
        return normalizeReference(value, "referencia obrigatoria");
    }

    private String normalizeReference(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
