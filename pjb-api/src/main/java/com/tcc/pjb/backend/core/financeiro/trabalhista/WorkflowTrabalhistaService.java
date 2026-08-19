package com.tcc.pjb.backend.core.financeiro.trabalhista;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.AcordoHomologadoResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalConsultaCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalConsultaResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalHealthQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalHealthResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.DepositoRecursalView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GerarGruTrabalhistaCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaConsultaCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaConsultaResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.HomologarAcordoTrabalhistaCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.RegistrarDepositoRecursalCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaAcordoHealthSnapshot;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaAcordoSnapshot;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaAcordoView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaConsultaTimelineCommand;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaConsultaTimelineResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaDepositaAuditSnapshot;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaFluxoStatusQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaFluxoStatusResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruHealthQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruHealthResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaGruHealthSnapshot;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaProcessoQuery;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaProcessoResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaProcessoView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaTimelineAuditSnapshot;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaTimelineEntry;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.financeiro.DepositoRecursal;
import com.tcc.pjb.backend.model.entity.financeiro.GruJudicialTrabalhista;
import com.tcc.pjb.backend.model.repository.DepositoRecursalRepository;
import com.tcc.pjb.backend.model.repository.GruJudicialTrabalhistaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowTrabalhistaService {

    private final ProcessoRepository processoRepository;
    private final GruJudicialTrabalhistaRepository gruRepository;
    private final DepositoRecursalRepository depositoRepository;
    private final GruCodigoBarrasGenerator gruTrabalhistaGenerator;
    private final TrabalhistaWorkflowProperties properties;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy rawPolicy;

    public WorkflowTrabalhistaService(ProcessoRepository processoRepository,
                                      GruJudicialTrabalhistaRepository gruRepository,
                                      DepositoRecursalRepository depositoRepository,
                                      @Qualifier("gruTrabalhistaGenerator") GruCodigoBarrasGenerator gruTrabalhistaGenerator,
                                      TrabalhistaWorkflowProperties properties,
                                      AuditLedgerService auditLedger,
                                      ReadAfterWriteConsistencyPolicy rawPolicy) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.gruRepository = Objects.requireNonNull(gruRepository);
        this.depositoRepository = Objects.requireNonNull(depositoRepository);
        this.gruTrabalhistaGenerator = Objects.requireNonNull(gruTrabalhistaGenerator);
        this.properties = Objects.requireNonNull(properties);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.rawPolicy = Objects.requireNonNull(rawPolicy);
    }

    @Transactional
    public GruTrabalhistaResult gerarGruRecursal(GerarGruTrabalhistaCommand command) {
        Objects.requireNonNull(command);
        return gerarGruRecursal(command.processoId(), command.tipo(), command.valor());
    }

    @Transactional
    public GruTrabalhistaResult gerarGruRecursal(Long processoId, String tipo, BigDecimal valor) {
        Processo processo = loadTrabalhista(processoId);
        var gru = gruTrabalhistaGenerator.gerar(tipo, valor, processo.getUf() != null ? processo.getUf() : "DF");
        GruJudicialTrabalhista entity = GruJudicialTrabalhista.builder()
                .processoId(processoId)
                .tipo(tipo)
                .valor(valor)
                .indiceAtualizacao(properties.indiceAtualizacaoDefault())
                .nossoNumero(gru.nossoNumero())
                .linhaDigitavel(gru.linhaDigitavel())
                .status("PENDENTE")
                .vencimento(LocalDate.now().plusDays(properties.prazoPagamentoGruDias()))
                .createdAt(Instant.now())
                .tribunalTrt(processo.getTribunal())
                .build();
        gruRepository.save(entity);
        rawPolicy.markWrite();
        auditLedger.appendSafely("TRABALHISTA_GRU_GERADA", "PROCESSO", String.valueOf(processoId), null, "tipo=" + tipo + " valor=" + valor + " vencimento=" + entity.getVencimento());
        return new GruTrabalhistaResult(entity.getId(), gru.linhaDigitavel(), gru.codigoBarras(), entity.getVencimento());
    }

    @Transactional
    public DepositoRecursalResult registrarDepositoRecursal(RegistrarDepositoRecursalCommand command) {
        Objects.requireNonNull(command);
        return registrarDepositoRecursal(command.processoId(), command.instancia(), command.valorDepositado(), command.comprovanteHash());
    }

    @Transactional
    public DepositoRecursalResult registrarDepositoRecursal(Long processoId,
                                                            String instancia,
                                                            BigDecimal valorDepositado,
                                                            String comprovanteHash) {
        Processo processo = loadTrabalhista(processoId);
        DepositoRecursal entity = DepositoRecursal.builder()
                .processoId(processoId)
                .instancia(instancia)
                .valorTeto(properties.depositoRecursalTetoDefault())
                .valorDepositado(valorDepositado)
                .dataDeposito(LocalDate.now())
                .comprovanteHash(comprovanteHash)
                .status(valorDepositado.compareTo(properties.depositoRecursalTetoDefault()) >= 0 ? "CONFIRMADO" : "PENDENTE_COMPLEMENTO")
                .createdAt(Instant.now())
                .build();
        depositoRepository.save(entity);
        processo.setStatusProcesso(StatusProcesso.EXECUCAO_TRABALHISTA);
        processoRepository.save(processo);
        rawPolicy.markWrite();
        auditLedger.appendSafely("TRABALHISTA_DEPOSITO_RECURSAL", "PROCESSO", String.valueOf(processoId), null, "instancia=" + instancia + " valor=" + valorDepositado + " status=" + entity.getStatus());
        return new DepositoRecursalResult(entity.getId(), entity.getValorDepositado(), entity.getDataDeposito(), entity.getStatus());
    }

    @Transactional
    public AcordoHomologadoResult homologarAcordo(HomologarAcordoTrabalhistaCommand command) {
        Objects.requireNonNull(command);
        return homologarAcordo(command.processoId(), command.resumo());
    }

    @Transactional
    public AcordoHomologadoResult homologarAcordo(Long processoId, String resumo) {
        Processo processo = loadTrabalhista(processoId);
        processo.setStatusProcesso(StatusProcesso.ACORDO_HOMOLOGADO);
        processo.setResultadoFinal(resumo);
        processoRepository.save(processo);
        rawPolicy.markWrite();
        auditLedger.appendSafely("TRABALHISTA_ACORDO_HOMOLOGADO", "PROCESSO", String.valueOf(processoId), null, "resumo=" + resumo);
        return new AcordoHomologadoResult(processoId, StatusProcesso.ACORDO_HOMOLOGADO.name(), resumo);
    }

    @Transactional(readOnly = true)
    public GruTrabalhistaConsultaResult consultarGru(GruTrabalhistaConsultaCommand command) {
        Objects.requireNonNull(command);
        GruJudicialTrabalhista entity = gruRepository.findById(command.gruId())
                .orElseThrow(() -> new IllegalArgumentException("GRU trabalhista não encontrada: " + command.gruId()));
        return new GruTrabalhistaConsultaResult(entity.getId(), entity.getTipo(), entity.getValor(), entity.getStatus(), entity.getLinhaDigitavel(), entity.getVencimento());
    }

    @Transactional(readOnly = true)
    public GruTrabalhistaView viewGru(Long gruId) {
        GruJudicialTrabalhista entity = gruRepository.findById(gruId)
                .orElseThrow(() -> new IllegalArgumentException("GRU trabalhista não encontrada: " + gruId));
        return new GruTrabalhistaView(entity.getId(), entity.getTipo(), entity.getStatus(), entity.getLinhaDigitavel(), entity.getVencimento());
    }

    @Transactional(readOnly = true)
    public DepositoRecursalConsultaResult consultarDeposito(DepositoRecursalConsultaCommand command) {
        Objects.requireNonNull(command);
        DepositoRecursal entity = depositoRepository.findById(command.depositoId())
                .orElseThrow(() -> new IllegalArgumentException("Depósito recursal não encontrado: " + command.depositoId()));
        return new DepositoRecursalConsultaResult(entity.getId(), entity.getInstancia(), entity.getValorTeto(), entity.getValorDepositado(), entity.getStatus(), entity.getDataDeposito());
    }

    @Transactional(readOnly = true)
    public DepositoRecursalView viewDeposito(Long depositoId) {
        DepositoRecursal entity = depositoRepository.findById(depositoId)
                .orElseThrow(() -> new IllegalArgumentException("Depósito recursal não encontrado: " + depositoId));
        return new DepositoRecursalView(entity.getId(), entity.getInstancia(), entity.getStatus(), entity.getValorDepositado(), entity.getComprovanteHash());
    }

    @Transactional(readOnly = true)
    public TrabalhistaAcordoSnapshot acordoSnapshot(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        return new TrabalhistaAcordoSnapshot(processo.getId(), processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name(), processo.getResultadoFinal());
    }

    @Transactional(readOnly = true)
    public TrabalhistaConsultaTimelineResult timeline(TrabalhistaConsultaTimelineCommand command) {
        Objects.requireNonNull(command);
        ArrayList<TrabalhistaTimelineEntry> entries = new ArrayList<>();
        for (GruJudicialTrabalhista gru : gruRepository.findByProcessoIdOrderByCreatedAtDesc(command.processoId())) {
            entries.add(new TrabalhistaTimelineEntry("GRU", gru.getTipo() + ":" + gru.getStatus()));
        }
        for (DepositoRecursal deposito : depositoRepository.findByProcessoIdOrderByCreatedAtDesc(command.processoId())) {
            entries.add(new TrabalhistaTimelineEntry("DEPOSITO", deposito.getInstancia() + ":" + deposito.getStatus()));
        }
        return new TrabalhistaConsultaTimelineResult(command.processoId(), List.copyOf(entries));
    }

    private Processo loadTrabalhista(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        if (processo.getRamoDireito() != RamoDireito.TRABALHISTA) {
            throw new IllegalStateException("Fluxo aplicável apenas a processo trabalhista");
        }
        return processo;
    }

    @Transactional(readOnly = true)
    public TrabalhistaGruHealthSnapshot gruHealth(Long gruId) {
        var gru = gruRepository.findById(gruId).orElseThrow(() -> new IllegalArgumentException("GRU trabalhista não encontrada: " + gruId));
        return new TrabalhistaGruHealthSnapshot(gru.getId(), gru.getStatus(), "PENDENTE".equalsIgnoreCase(gru.getStatus()));
    }

    @Transactional(readOnly = true)
    public TrabalhistaDepositaAuditSnapshot depositoAudit(Long depositoId) {
        var deposito = depositoRepository.findById(depositoId).orElseThrow(() -> new IllegalArgumentException("Depósito recursal não encontrado: " + depositoId));
        return new TrabalhistaDepositaAuditSnapshot(deposito.getId(), deposito.getStatus(), deposito.getValorDepositado());
    }

    @Transactional(readOnly = true)
    public TrabalhistaAcordoView acordoView(Long processoId) {
        var processo = processoRepository.findById(processoId).orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        return new TrabalhistaAcordoView(processoId, StatusProcesso.ACORDO_HOMOLOGADO.equals(processo.getStatusProcesso()), processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name());
    }

    @Transactional(readOnly = true)
    public TrabalhistaProcessoView processoView(Long processoId) {
        var processo = processoRepository.findById(processoId).orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        return new TrabalhistaProcessoView(processoId, processo.getTribunal(), processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name());
    }

    @Transactional(readOnly = true)
    public TrabalhistaProcessoView health(com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaHealthQuery query) {
        Objects.requireNonNull(query);
        return processoView(query.processoId());
    }

    @Transactional(readOnly = true)
    public TrabalhistaFluxoStatusResult fluxoStatus(TrabalhistaFluxoStatusQuery query) {
        Objects.requireNonNull(query);
        var processo = processoRepository.findById(query.processoId()).orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + query.processoId()));
        boolean possuiGru = !gruRepository.findByProcessoIdOrderByCreatedAtDesc(query.processoId()).isEmpty();
        boolean possuiDeposito = !depositoRepository.findByProcessoIdOrderByCreatedAtDesc(query.processoId()).isEmpty();
        return new TrabalhistaFluxoStatusResult(query.processoId(), processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name(), StatusProcesso.ACORDO_HOMOLOGADO.equals(processo.getStatusProcesso()), possuiGru, possuiDeposito);
    }

    @Transactional(readOnly = true)
    public TrabalhistaGruHealthResult gruHealth(TrabalhistaGruHealthQuery query) {
        Objects.requireNonNull(query);
        var gru = gruRepository.findById(query.gruId()).orElseThrow(() -> new IllegalArgumentException("GRU trabalhista não encontrada: " + query.gruId()));
        return new TrabalhistaGruHealthResult(gru.getId(), gru.getStatus(), "PENDENTE".equalsIgnoreCase(gru.getStatus()), gru.getVencimento());
    }

    @Transactional(readOnly = true)
    public DepositoRecursalHealthResult depositoHealth(DepositoRecursalHealthQuery query) {
        Objects.requireNonNull(query);
        var deposito = depositoRepository.findById(query.depositoId()).orElseThrow(() -> new IllegalArgumentException("Depósito recursal não encontrado: " + query.depositoId()));
        return new DepositoRecursalHealthResult(deposito.getId(), deposito.getStatus(), deposito.getValorDepositado(), "CONFIRMADO".equalsIgnoreCase(deposito.getStatus()));
    }

    @Transactional(readOnly = true)
    public TrabalhistaProcessoResult processoResult(TrabalhistaProcessoQuery query) {
        Objects.requireNonNull(query);
        return new TrabalhistaProcessoResult(processoView(query.processoId()), fluxoStatus(new TrabalhistaFluxoStatusQuery(query.processoId())));
    }

    @Transactional(readOnly = true)
    public TrabalhistaTimelineAuditSnapshot timelineAudit(Long processoId) {
        var timeline = timeline(new TrabalhistaConsultaTimelineCommand(processoId));
        long grupos = timeline.entries().stream().filter(e -> "GRU".equalsIgnoreCase(e.etapa())).count();
        long depositos = timeline.entries().stream().filter(e -> "DEPOSITO".equalsIgnoreCase(e.etapa())).count();
        return new TrabalhistaTimelineAuditSnapshot(processoId, timeline.entries().size(), grupos > 0, depositos > 0);
    }

    @Transactional(readOnly = true)
    public TrabalhistaAcordoHealthSnapshot acordoHealth(Long processoId) {
        var acordo = acordoView(processoId);
        return new TrabalhistaAcordoHealthSnapshot(processoId, acordo.homologado(), acordo.statusProcesso());
    }
}
