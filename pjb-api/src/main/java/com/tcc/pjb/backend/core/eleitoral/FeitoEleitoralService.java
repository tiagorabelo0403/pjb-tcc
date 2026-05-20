package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.eleitoral.domain.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.eleitoral.FeitoEleitoralEspecial;
import com.tcc.pjb.backend.model.entity.eleitoral.CalendarioEleitoral;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.CalendarioEleitoralRepository;
import com.tcc.pjb.backend.model.repository.FeitoEleitoralEspecialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeitoEleitoralService {

    private final ProcessoRepository processoRepository;
    private final FeitoEleitoralEspecialRepository feitoRepository;
    private final CalendarioEleitoralRepository calendarioRepository;
    private final AuditLedgerService auditLedger;
    private final EleitoralTseProperties properties;
    private final TseSpcaClient spcaClient;

    @Inject
    public FeitoEleitoralService(ProcessoRepository processoRepository,
                                 FeitoEleitoralEspecialRepository feitoRepository,
                                 CalendarioEleitoralRepository calendarioRepository,
                                 AuditLedgerService auditLedger,
                                 EleitoralTseProperties properties) {
        this(processoRepository, feitoRepository, calendarioRepository, auditLedger, properties,
                (processoId, numeroCandidato, anoEleitoral) -> new PrestacaoContasSnapshot(
                        processoId,
                        numeroCandidato,
                        anoEleitoral,
                        "SEM_INTEGRACAO",
                        null,
                        "cliente SPCA não configurado"));
    }

    FeitoEleitoralService(ProcessoRepository processoRepository,
                          FeitoEleitoralEspecialRepository feitoRepository,
                          CalendarioEleitoralRepository calendarioRepository,
                          AuditLedgerService auditLedger,
                          EleitoralTseProperties properties,
                          TseSpcaClient spcaClient) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.feitoRepository = Objects.requireNonNull(feitoRepository);
        this.calendarioRepository = Objects.requireNonNull(calendarioRepository);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.properties = Objects.requireNonNull(properties);
        this.spcaClient = Objects.requireNonNull(spcaClient);
    }

    @Transactional
    public ExtincaoEleitoralResult registrarDiplomacao(RegistrarDiplomacaoCommand command) {
        Objects.requireNonNull(command);
        return registrarDiplomacao(command.processoId(), command.dataDiplomacao());
    }

    @Transactional
    public ExtincaoEleitoralResult registrarDiplomacao(Long processoId, LocalDate dataDiplomacao) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        if (processo.getRamoDireito() != RamoDireito.ELEITORAL) {
            throw new IllegalStateException("registrarDiplomacao só aplicável a feitos eleitorais");
        }
        FeitoEleitoralEspecial feito = loadFeitoOptional(processoId);
        if (feito == null) {
            return ExtincaoEleitoralResult.semFeito(processoId);
        }
        feito.setDiplomadoEm(dataDiplomacao);
        feito.setStatusEleitoral("DIPLOMADO");
        if (autoExtincaoEnabled() && deveExtinguirPosDiplomacao(feito.getTipoFeito())) {
            feito.setExtintoEm(Instant.now());
            feito.setMotivoExtincao("Diplomação sem impugnação pendente — extinção automática");
            feito.setStatusEleitoral("EXTINTO");
            processo.setStatusProcesso(StatusProcesso.ARQUIVADO);
            processoRepository.save(processo);
        }
        feitoRepository.save(feito);
        auditLedger.appendSafely(
                "FEITO_ELEITORAL_DIPLOMACAO",
                "PROCESSO",
                String.valueOf(processoId),
                "tipo=" + feito.getTipoFeito() + " data=" + dataDiplomacao + " dryRun=" + properties.dryRun());
        return new ExtincaoEleitoralResult(processoId, feito.getExtintoEm() != null, feito.getMotivoExtincao());
    }

    @Transactional
    public SincronizarPrestacaoContasResult sincronizarPrestacaoContasResult(SincronizarPrestacaoContasCommand command) {
        Objects.requireNonNull(command);
        PrestacaoContasSnapshot snapshot = sincronizarPrestacaoContas(command.processoId());
        return new SincronizarPrestacaoContasResult(command.processoId(), snapshot.status(), snapshot.protocoloExterno());
    }

    @Transactional
    public SincronizarPrestacaoContasResult sincronizarPrestacaoContasResult(Long processoId) {
        PrestacaoContasSnapshot snapshot = sincronizarPrestacaoContas(processoId);
        return new SincronizarPrestacaoContasResult(processoId, snapshot.status(), snapshot.protocoloExterno());
    }

    @Transactional
    public PrestacaoContasSnapshot sincronizarPrestacaoContas(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        PrestacaoContasSnapshot snapshot = spcaClient.consultarPrestacaoContas(
                processoId,
                feito.getNumeroCandidato(),
                feito.getAnoEleitoral() == null ? null : String.valueOf(feito.getAnoEleitoral()));
        auditLedger.appendSafely(
                "ELEITORAL_SPCA_SYNC",
                "PROCESSO",
                String.valueOf(processoId),
                "status=" + snapshot.status() + " protocolo=" + snapshot.protocoloExterno());
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<FeitoEleitoralPendenteDiplomacaoView> listarFeitosPendentesDeDiplomacao(ListarFeitosPendentesDiplomacaoCommand command) {
        Objects.requireNonNull(command);
        int limit = Math.max(1, command.limit());
        return listarFeitosPendentesDeDiplomacao().stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public List<FeitoEleitoralPendenteDiplomacaoView> listarFeitosPendentesDeDiplomacao() {
        return feitoRepository.findTop100ByStatusEleitoralIgnoreCaseAndDiplomadoEmIsNullOrderByIdAsc("EM_ANDAMENTO")
                .stream()
                .map(feito -> new FeitoEleitoralPendenteDiplomacaoView(
                        feito.getProcessoId(),
                        feito.getTipoFeito(),
                        feito.getNumeroCandidato(),
                        feito.getAnoEleitoral()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeitoEleitoralStatusSnapshot statusSnapshot(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeitoOptional(processoId);
        if (feito == null) {
            return new FeitoEleitoralStatusSnapshot(processoId, "NAO_ENCONTRADO", null);
        }
        return new FeitoEleitoralStatusSnapshot(processoId, safeStatus(feito.getStatusEleitoral()), feito.getDiplomadoEm());
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioSnapshot calendarioSnapshot(String uf, LocalDate data) {
        CalendarioEleitoral calendario = calendarioRepository.findApplicableWindows(uf, null, data).stream().findFirst().orElse(null);
        if (calendario == null) {
            return new EleitoralCalendarioSnapshot(uf, data, data, "SEM_JANELA");
        }
        return new EleitoralCalendarioSnapshot(uf, calendario.getDataInicio(), calendario.getDataFim(), calendario.getFase());
    }

    @Transactional(readOnly = true)
    public JanelaEleitoralConsultaResult consultarJanelaEleitoral(JanelaEleitoralConsulta consulta) {
        Objects.requireNonNull(consulta);
        return new JanelaEleitoralConsultaResult(consulta.uf(), estaNaJanelaEleitoral(consulta.uf(), consulta.data()));
    }

    @Transactional(readOnly = true)
    public boolean estaNaJanelaEleitoral(String uf, LocalDate data) {
        return calendarioRepository.existsByUfAndDataBetween(uf, data);
    }

    @Transactional(readOnly = true)
    public EleitoralPendenciaDiplomacaoSnapshot pendenciaDiplomacao(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralPendenciaDiplomacaoSnapshot(processoId, feito.getTipoFeito(), safeStatus(feito.getStatusEleitoral()));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasProjection prestacaoContasProjection(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralPrestacaoContasProjection(processoId, feito.getPartidoSigla(), feito.getCargo(), safeStatus(feito.getStatusEleitoral()));
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioConsultaResult consultarCalendario(EleitoralCalendarioConsultaCommand command) {
        Objects.requireNonNull(command);
        EleitoralCalendarioSnapshot snapshot = calendarioSnapshot(command.uf(), command.data());
        return new EleitoralCalendarioConsultaResult(snapshot, estaNaJanelaEleitoral(command.uf(), command.data()));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasAuditSnapshot prestacaoContasAudit(Long processoId) {
        EleitoralPrestacaoContasProjection projection = prestacaoContasProjection(processoId);
        return new EleitoralPrestacaoContasAuditSnapshot(
                projection.processoId(),
                projection.partidoSigla(),
                projection.cargo(),
                projection.statusEleitoral());
    }

    @Transactional(readOnly = true)
    public EleitoralConsultaFeitoResult consultarFeito(EleitoralConsultaFeitoCommand command) {
        Objects.requireNonNull(command);
        return new EleitoralConsultaFeitoResult(feitoSnapshot(command.processoId()), pendenciaDiplomacao(command.processoId()));
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoAuditSnapshot diplomacaoAudit(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralDiplomacaoAuditSnapshot(processoId, feito.getTipoFeito(), feito.getDiplomadoEm(), feito.getExtintoEm() != null);
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasStatusView prestacaoContasStatus(Long processoId) {
        EleitoralPrestacaoContasProjection projection = prestacaoContasProjection(processoId);
        return new EleitoralPrestacaoContasStatusView(projection.processoId(), projection.partidoSigla(), projection.statusEleitoral());
    }

    @Transactional(readOnly = true)
    public EleitoralJanelaSnapshot janelaSnapshot(String uf, LocalDate data) {
        return new EleitoralJanelaSnapshot(uf, data, estaNaJanelaEleitoral(uf, data));
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoView feitoView(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralFeitoView(processoId, feito.getTipoFeito(), safeStatus(feito.getStatusEleitoral()), feito.getDiplomadoEm());
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoTimelineResult timeline(EleitoralFeitoTimelineQuery query) {
        Objects.requireNonNull(query);
        FeitoEleitoralEspecial feito = loadFeito(query.processoId());
        List<EleitoralFeitoTimelineEntry> entries = new ArrayList<>();
        entries.add(new EleitoralFeitoTimelineEntry("CRIADO", safeInstant(feito.getCreatedAt()), feito.getTipoFeito()));
        if (feito.getDiplomadoEm() != null) {
            entries.add(new EleitoralFeitoTimelineEntry(
                    "DIPLOMADO",
                    feito.getDiplomadoEm().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    safeStatus(feito.getStatusEleitoral())));
        }
        if (feito.getExtintoEm() != null) {
            entries.add(new EleitoralFeitoTimelineEntry("EXTINTO", feito.getExtintoEm(), feito.getMotivoExtincao()));
        }
        return new EleitoralFeitoTimelineResult(query.processoId(), List.copyOf(entries));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasView prestacaoContasView(Long processoId) {
        EleitoralPrestacaoContasProjection projection = prestacaoContasProjection(processoId);
        return new EleitoralPrestacaoContasView(
                projection.processoId(),
                projection.partidoSigla(),
                projection.cargo(),
                projection.statusEleitoral());
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioView calendarioView(EleitoralCalendarioQuery query) {
        Objects.requireNonNull(query);
        EleitoralCalendarioSnapshot snapshot = calendarioSnapshot(query.uf(), query.data());
        return new EleitoralCalendarioView(snapshot.uf(), query.data(), snapshot.fase());
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoResult diplomacao(EleitoralDiplomacaoQuery query) {
        Objects.requireNonNull(query);
        return new EleitoralDiplomacaoResult(feitoView(query.processoId()), diplomacaoAudit(query.processoId()));
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasView prestacaoContas(EleitoralPrestacaoContasQuery query) {
        Objects.requireNonNull(query);
        return prestacaoContasView(query.processoId());
    }

    @Transactional(readOnly = true)
    public EleitoralHealthResult health(EleitoralHealthQuery query) {
        Objects.requireNonNull(query);
        EleitoralFeitoView feito = feitoView(query.processoId());
        EleitoralJanelaSnapshot janela = janelaSnapshot(query.uf(), query.dataConsulta());
        return new EleitoralHealthResult(feito, janela, !"EXTINTO".equalsIgnoreCase(feito.status()));
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoHealthView feitoHealthView(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralFeitoHealthView(processoId, feito.getTipoFeito(), safeStatus(feito.getStatusEleitoral()), feito.getDiplomadoEm() != null, feito.getExtintoEm() != null);
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasHealthSnapshot prestacaoContasHealth(Long processoId) {
        EleitoralPrestacaoContasProjection projection = prestacaoContasProjection(processoId);
        return new EleitoralPrestacaoContasHealthSnapshot(
                projection.processoId(),
                projection.partidoSigla(),
                projection.statusEleitoral(),
                !"EXTINTO".equalsIgnoreCase(projection.statusEleitoral()));
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoView diplomacaoView(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralDiplomacaoView(processoId, feito.getTipoFeito(), feito.getDiplomadoEm(), feito.getExtintoEm() != null);
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoHealthSnapshot diplomacaoHealth(Long processoId) {
        EleitoralDiplomacaoView view = diplomacaoView(processoId);
        return new EleitoralDiplomacaoHealthSnapshot(processoId, view.diplomadoEm() != null, view.extinto(), feitoView(processoId).status());
    }

    @Transactional(readOnly = true)
    public EleitoralPendenteDiplomacaoResult pendentes(EleitoralPendenteDiplomacaoQuery query) {
        Objects.requireNonNull(query);
        return new EleitoralPendenteDiplomacaoResult(listarFeitosPendentesDeDiplomacao(new ListarFeitosPendentesDiplomacaoCommand(query.limit())));
    }

    @Transactional(readOnly = true)
    public EleitoralTimelineAuditSnapshot timelineAudit(Long processoId) {
        EleitoralFeitoTimelineResult timeline = timeline(new EleitoralFeitoTimelineQuery(processoId));
        boolean diplomado = timeline.entries().stream().anyMatch(entry -> "DIPLOMADO".equalsIgnoreCase(entry.evento()));
        boolean extinto = timeline.entries().stream().anyMatch(entry -> "EXTINTO".equalsIgnoreCase(entry.evento()));
        return new EleitoralTimelineAuditSnapshot(processoId, timeline.entries().size(), diplomado, extinto);
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioHealthView calendarioHealth(EleitoralCalendarioQuery query) {
        Objects.requireNonNull(query);
        EleitoralCalendarioSnapshot snapshot = calendarioSnapshot(query.uf(), query.data());
        return new EleitoralCalendarioHealthView(snapshot.uf(), query.data(), estaNaJanelaEleitoral(query.uf(), query.data()), snapshot.fase());
    }

    @Transactional(readOnly = true)
    public EleitoralCargoView cargoView(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralCargoView(processoId, feito.getCargo(), feito.getNumeroCandidato(), feito.getPartidoSigla());
    }

    @Transactional(readOnly = true)
    public EleitoralPartidoView partidoView(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralPartidoView(feito.getPartidoSigla(), feito.getAnoEleitoral() == null ? 0 : feito.getAnoEleitoral(), feito.getTipoFeito());
    }

    @Transactional(readOnly = true)
    public EleitoralResultadoWindowResult resultadoWindow(EleitoralResultadoWindowQuery query) {
        Objects.requireNonNull(query);
        List<CalendarioEleitoral> windows = calendarioRepository.findApplicableWindows(query.uf(), query.fase(), query.dataReferencia());
        if (windows.isEmpty()) {
            return new EleitoralResultadoWindowResult(query.uf(), query.fase(), query.dataReferencia(), query.dataReferencia(), false);
        }
        CalendarioEleitoral selected = windows.getFirst();
        return new EleitoralResultadoWindowResult(query.uf(), query.fase(), selected.getDataInicio(), selected.getDataFim(), true);
    }

    @Transactional(readOnly = true)
    public EleitoralResultadoWindowView resultadoWindowView(EleitoralResultadoWindowQuery query) {
        EleitoralResultadoWindowResult result = resultadoWindow(query);
        return new EleitoralResultadoWindowView(result.uf(), result.inicio(), result.fim(), properties.dryRun());
    }

    @Transactional(readOnly = true)
    public EleitoralSilencioView silencio(EleitoralSilencioQuery query) {
        Objects.requireNonNull(query);
        boolean emJanelaEleitoral = !query.tutelaUrgente() && estaNaJanelaEleitoral(query.uf(), query.data());
        return new EleitoralSilencioView(query.uf(), query.data(), emJanelaEleitoral);
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoConsistencyView feitoConsistencyView(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        return new EleitoralFeitoConsistencyView(String.valueOf(processoId), safeStatus(feito.getStatusEleitoral()), Instant.now());
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasConsistencyView prestacaoContasConsistencyView(Long processoId) {
        EleitoralPrestacaoContasProjection projection = prestacaoContasProjection(processoId);
        return new EleitoralPrestacaoContasConsistencyView(String.valueOf(processoId), projection.statusEleitoral(), Instant.now());
    }

    @Transactional(readOnly = true)
    public EleitoralFeitoOwnershipView ownershipView(Long processoId) {
        EleitoralFeitoView view = feitoView(processoId);
        return new EleitoralFeitoOwnershipView(String.valueOf(processoId), view.status(), view.tipoFeito());
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioWindowView calendarioWindowView(String uf, LocalDate data) {
        EleitoralCalendarioSnapshot snapshot = calendarioSnapshot(uf, data);
        return new EleitoralCalendarioWindowView(uf, snapshot.fase(), Instant.now());
    }

    @Transactional(readOnly = true)
    public EleitoralCalendarioWindowAuditView calendarioWindowAudit(String uf, LocalDate data) {
        EleitoralCalendarioSnapshot snapshot = calendarioSnapshot(uf, data);
        return new EleitoralCalendarioWindowAuditView(uf, snapshot.fase(), "inicio=" + snapshot.dataInicio() + " fim=" + snapshot.dataFim());
    }

    @Transactional(readOnly = true)
    public EleitoralPrestacaoContasWindowView prestacaoContasWindowView(Long processoId) {
        EleitoralPrestacaoContasProjection projection = prestacaoContasProjection(processoId);
        return new EleitoralPrestacaoContasWindowView(String.valueOf(processoId), projection.statusEleitoral(), projection.partidoSigla());
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoWindowView diplomacaoWindowView(Long processoId) {
        EleitoralDiplomacaoView view = diplomacaoView(processoId);
        String status = view.extinto() ? "EXTINTO" : view.diplomadoEm() != null ? "DIPLOMADO" : "PENDENTE";
        return new EleitoralDiplomacaoWindowView(String.valueOf(processoId), status, Instant.now());
    }

    @Transactional(readOnly = true)
    public EleitoralDiplomacaoWindowAuditView diplomacaoWindowAudit(Long processoId) {
        EleitoralDiplomacaoView view = diplomacaoView(processoId);
        String status = view.extinto() ? "EXTINTO" : view.diplomadoEm() != null ? "DIPLOMADO" : "PENDENTE";
        return new EleitoralDiplomacaoWindowAuditView(String.valueOf(processoId), status, view.tipoFeito());
    }

    private FeitoEleitoralStatusSnapshot feitoStatus(Long processoId) {
        return statusSnapshot(processoId);
    }

    private EleitoralFeitoSnapshot feitoSnapshot(Long processoId) {
        FeitoEleitoralEspecial feito = loadFeito(processoId);
        FeitoEleitoralStatusSnapshot status = feitoStatus(processoId);
        return new EleitoralFeitoSnapshot(processoId, feito.getTipoFeito(), status.statusEleitoral(), status.diplomadoEm());
    }

    private boolean autoExtincaoEnabled() {
        return properties.diplomacao() == null || properties.diplomacao().autoExtincaoEnabled();
    }

    private boolean deveExtinguirPosDiplomacao(String tipoFeito) {
        return "RCED".equalsIgnoreCase(tipoFeito) || "AIME".equalsIgnoreCase(tipoFeito);
    }

    private FeitoEleitoralEspecial loadFeito(Long processoId) {
        return feitoRepository.findByProcessoId(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Feito eleitoral não encontrado: " + processoId));
    }

    private FeitoEleitoralEspecial loadFeitoOptional(Long processoId) {
        return feitoRepository.findByProcessoId(processoId).orElse(null);
    }

    private Instant safeInstant(Instant instant) {
        return instant == null ? Instant.now() : instant;
    }

    private String safeStatus(String status) {
        return status == null || status.isBlank() ? "SEM_STATUS" : status;
    }
}
