package com.tcc.pjb.backend.core.criminal.custodia;

import com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.AudienciaCustodiaResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCustodiaResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPrazoConsultaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPrazoConsultaResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaTimelineEntry;
import com.tcc.pjb.backend.core.criminal.custodia.domain.MedidaCautelarRegistroSnapshot;
import com.tcc.pjb.backend.core.criminal.custodia.domain.RegistrarPrisaoCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ResultadoCustodia;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.criminal.AudienciaCustodia;
import com.tcc.pjb.backend.model.entity.criminal.BnmpConsultaLog;
import com.tcc.pjb.backend.model.entity.criminal.MedidaCautelar;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.BnmpConsultaLogRepository;
import com.tcc.pjb.backend.model.repository.MedidaCautelarRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaAndamentoSnapshot;
import com.tcc.pjb.backend.core.criminal.custodia.domain.MedidaCautelarSnapshot;

@Service
public class AudienciaCustodiaService {

    private final ProcessoRepository processoRepository;
    private final AudienciaCustodiaRepository custodiaRepository;
    private final MedidaCautelarRepository medidaCautelarRepository;
    private final BnmpConsultaLogRepository bnmpConsultaLogRepository;
    private final BnmpConsultaService bnmpConsultaService;
    private final CurrentUserService currentUserService;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;
    private final AuditLedgerService auditLedger;

    public AudienciaCustodiaService(ProcessoRepository processoRepository,
                                    AudienciaCustodiaRepository custodiaRepository,
                                    MedidaCautelarRepository medidaCautelarRepository,
                                    BnmpConsultaLogRepository bnmpConsultaLogRepository,
                                    BnmpConsultaService bnmpConsultaService,
                                    CurrentUserService currentUserService,
                                    ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy,
                                    AuditLedgerService auditLedger) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.custodiaRepository = Objects.requireNonNull(custodiaRepository);
        this.medidaCautelarRepository = Objects.requireNonNull(medidaCautelarRepository);
        this.bnmpConsultaLogRepository = Objects.requireNonNull(bnmpConsultaLogRepository);
        this.bnmpConsultaService = Objects.requireNonNull(bnmpConsultaService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @Transactional
    public AudienciaCustodiaResult registrarPrisao(RegistrarPrisaoCommand cmd) {
        Processo processo = loadPenalProcess(cmd.processoId());
        Instant dataPrisao = cmd.dataPrisao() != null ? cmd.dataPrisao() : Instant.now();
        Instant prazo24h = dataPrisao.plus(24, ChronoUnit.HOURS);
        AudienciaCustodia entity = AudienciaCustodia.builder()
                .processoId(processo.getId())
                .presoNome(cmd.presoNome())
                .presoCpf(cmd.presoCpf())
                .dataPrisao(dataPrisao)
                .prazoLimite24h(prazo24h)
                .status("PENDENTE")
                .createdAt(Instant.now())
                .build();
        custodiaRepository.save(entity);
        processo.setStatusProcesso(StatusProcesso.AUDIENCIA_CUSTODIA_PENDENTE);
        processoRepository.save(processo);
        readAfterWriteConsistencyPolicy.markWrite();
        auditLedger.appendSafely("CUSTODIA_PRISAO_REGISTRADA", "PROCESSO", String.valueOf(processo.getId()), "preso=" + cmd.presoNome() + " prazo=" + prazo24h);
        return new AudienciaCustodiaResult(entity.getId(), prazo24h);
    }

    @Transactional
    public ResultadoCustodia concluirAudiencia(ConcluirAudienciaCommand cmd) {
        AudienciaCustodia custodia = custodiaRepository.findById(cmd.custodiaId())
                .orElseThrow(() -> new IllegalArgumentException("Audiência de custódia não encontrada: " + cmd.custodiaId()));
        Processo processo = processoRepository.findById(custodia.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + custodia.getProcessoId()));
        BnmpConsultaResult bnmp = bnmpConsultaService.consultarMandadoAtivo(custodia.getPresoCpf());
        bnmpConsultaLogRepository.save(BnmpConsultaLog.builder()
                .processoId(processo.getId())
                .cpfConsultado(custodia.getPresoCpf())
                .mandadoAtivo(bnmp.mandadoAtivo())
                .numeroMandado(bnmp.numeroMandado())
                .consultadoEm(Instant.now())
                .operadorId(currentUserService.currentUserIdOrZero())
                .build());
        custodia.setResultado(cmd.resultado());
        custodia.setRealizadaEm(Instant.now());
        custodia.setStatus("REALIZADA");
        custodia.setMagistradoId(currentUserService.currentUserIdOrZero());
        custodia.setMedidasCautelares(String.join("; ", cmd.medidasCautelares()));
        custodiaRepository.save(custodia);
        if ("PRISAO_PREVENTIVA".equalsIgnoreCase(cmd.resultado())) {
            processo.setStatusProcesso(StatusProcesso.PRESO_PROVISORIO);
        } else {
            processo.setStatusProcesso(StatusProcesso.LIBERDADE_PROVISORIA);
        }
        processoRepository.save(processo);
        if (cmd.medidasCautelares() != null) {
            for (String medida : cmd.medidasCautelares()) {
                if (medida == null || medida.isBlank()) {
                    continue;
                }
                medidaCautelarRepository.save(MedidaCautelar.builder()
                        .processoId(processo.getId())
                        .tipo(medida.trim())
                        .descricao(medida.trim())
                        .ativa(true)
                        .createdAt(Instant.now())
                        .build());
            }
        }
        readAfterWriteConsistencyPolicy.markWrite();
        auditLedger.appendSafely("CUSTODIA_AUDIENCIA_REALIZADA", "PROCESSO", String.valueOf(processo.getId()), "resultado=" + cmd.resultado() + " bnmpMandadoAtivo=" + bnmp.mandadoAtivo());
        return new ResultadoCustodia(custodia.getId(), processo.getStatusProcesso().name(), bnmp.mandadoAtivo(), bnmp.numeroMandado());
    }


    @Transactional
    public ConcluirAudienciaCustodiaResult concluirAudienciaResumo(ConcluirAudienciaCommand cmd) {
        ResultadoCustodia resultado = concluirAudiencia(cmd);
        return new ConcluirAudienciaCustodiaResult(resultado.custodiaId(), resultado.statusProcesso(), resultado.mandadoAtivoBnmp(), resultado.numeroMandado());
    }

    private Processo loadPenalProcess(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        if (processo.getRamoDireito() != RamoDireito.PENAL && processo.getRamoDireito() != RamoDireito.MILITAR) {
            throw new IllegalStateException("Audiência de custódia apenas para processos penais/militares");
        }
        return processo;
    }


    @Transactional(readOnly = true)
    public CustodiaAndamentoSnapshot andamento(Long custodiaId) {
        AudienciaCustodia custodia = custodiaRepository.findById(custodiaId)
                .orElseThrow(() -> new IllegalArgumentException("Audiência de custódia não encontrada: " + custodiaId));
        return new CustodiaAndamentoSnapshot(custodia.getId(), custodia.getStatus(), custodia.getPrazoLimite24h(), custodia.getRealizadaEm());
    }

    @Transactional(readOnly = true)
    public java.util.List<MedidaCautelarSnapshot> medidas(Long processoId) {
        return medidaCautelarRepository.findByProcessoIdAndAtivaTrueOrderByProximoComparecimentoAsc(processoId).stream()
                .map(medida -> new MedidaCautelarSnapshot(medida.getId(), medida.getTipo(), medida.isAtiva(), medida.getProximoComparecimento()))
                .toList();
    }


    @Transactional(readOnly = true)
    public CustodiaConsultaResult consultar(CustodiaConsultaCommand command) {
        Objects.requireNonNull(command);
        AudienciaCustodia entity = custodiaRepository.findById(command.custodiaId())
                .orElseThrow(() -> new IllegalArgumentException("Custódia não encontrada: " + command.custodiaId()));
        return new CustodiaConsultaResult(entity.getId(), entity.getProcessoId(), entity.getStatus(), entity.getResultado(), entity.getPrazoLimite24h(), entity.getRealizadaEm());
    }

    @Transactional(readOnly = true)
    public CustodiaPrazoConsultaResult consultarPrazo(CustodiaPrazoConsultaCommand command) {
        Objects.requireNonNull(command);
        AudienciaCustodia entity = custodiaRepository.findById(command.custodiaId())
                .orElseThrow(() -> new IllegalArgumentException("Custódia não encontrada: " + command.custodiaId()));
        Instant now = Instant.now();
        Instant limite = entity.getPrazoLimite24h();
        java.time.Duration restante = limite == null ? java.time.Duration.ZERO : java.time.Duration.between(now, limite);
        return new CustodiaPrazoConsultaResult(entity.getId(), entity.getDataPrisao(), limite, restante.isNegative() ? java.time.Duration.ZERO : restante, limite != null && now.isAfter(limite));
    }

    @Transactional(readOnly = true)
    public List<CustodiaTimelineEntry> timeline(Long custodiaId) {
        AudienciaCustodia entity = custodiaRepository.findById(custodiaId)
                .orElseThrow(() -> new IllegalArgumentException("Custódia não encontrada: " + custodiaId));
        java.util.ArrayList<CustodiaTimelineEntry> timeline = new java.util.ArrayList<>();
        timeline.add(new CustodiaTimelineEntry("PRISAO_REGISTRADA", entity.getDataPrisao(), entity.getPresoNome()));
        timeline.add(new CustodiaTimelineEntry("PRAZO_CUSTODIA", entity.getPrazoLimite24h(), entity.getStatus()));
        if (entity.getRealizadaEm() != null) {
            timeline.add(new CustodiaTimelineEntry("AUDIENCIA_REALIZADA", entity.getRealizadaEm(), entity.getResultado()));
        }
        return java.util.List.copyOf(timeline);
    }

    @Transactional(readOnly = true)
    public MedidaCautelarRegistroSnapshot medidaSnapshot(Long medidaId) {
        MedidaCautelar medida = medidaCautelarRepository.findById(medidaId)
                .orElseThrow(() -> new IllegalArgumentException("Medida cautelar não encontrada: " + medidaId));
        return new MedidaCautelarRegistroSnapshot(medida.getId(), medida.getProcessoId(), medida.getTipo(), medida.isAtiva(), medida.getProximoComparecimento());
    }



    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineResult consultarTimeline(com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineCommand command) {
        Objects.requireNonNull(command);
        return new com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineResult(command.custodiaId(), timeline(command.custodiaId()));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaMedidaCautelarView medidaView(Long medidaId) {
        var snapshot = medidaSnapshot(medidaId);
        return new com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaMedidaCautelarView(snapshot.id(), snapshot.processoId(), snapshot.tipo(), snapshot.ativa(), snapshot.proximoComparecimento());
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaResultadoSnapshot resultadoSnapshot(Long custodiaId) {
        var entity = custodiaRepository.findById(custodiaId)
                .orElseThrow(() -> new IllegalArgumentException("Custódia não encontrada: " + custodiaId));
        return new com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaResultadoSnapshot(entity.getId(), entity.getResultado(), entity.getRealizadaEm());
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaAuditoriaSnapshot auditoria(Long custodiaId) {
        var entity = custodiaRepository.findById(custodiaId)
                .orElseThrow(() -> new IllegalArgumentException("Custódia não encontrada: " + custodiaId));
        return new com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaAuditoriaSnapshot(entity.getId(), entity.getProcessoId(), entity.getStatus(), Instant.now());
    }

}
