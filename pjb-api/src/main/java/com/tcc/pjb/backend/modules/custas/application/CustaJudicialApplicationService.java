package com.tcc.pjb.backend.modules.custas.application;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.modules.custas.api.CustaJudicialStorePort;
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaContexto;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaPort;
import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadGenerator;
import com.tcc.pjb.backend.modules.custas.domain.CustaJudicialResult;
import com.tcc.pjb.backend.modules.custas.domain.RegistrarPagamentoCustaResult;
import com.tcc.pjb.backend.modules.custas.domain.RegistrarPagamentoCustaCommand;
import com.tcc.pjb.backend.modules.custas.domain.GerarCustaJudicialCommand;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import com.tcc.pjb.backend.modules.custas.domain.PixResult;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaCommand;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaResult;
import com.tcc.pjb.backend.modules.custas.domain.CustaPagamentoCommandSnapshot;
import com.tcc.pjb.backend.modules.custas.domain.CustaPagamentoAuditSnapshot;
import com.tcc.pjb.backend.modules.custas.domain.GruEmissaoSnapshot;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadSnapshot;
import com.tcc.pjb.backend.modules.custas.domain.CustaTimelineEntry;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaTimelineCommand;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaTimelineResult;
import com.tcc.pjb.backend.modules.custas.domain.CustaStatusSnapshot;
import com.tcc.pjb.backend.modules.custas.domain.CustaVencimentoSnapshot;
import com.tcc.pjb.backend.modules.custas.domain.TipoCusta;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.modules.custas.domain.CustaJudicialView;
import com.tcc.pjb.backend.modules.custas.domain.PixCobrancaView;

@Service
public class CustaJudicialApplicationService {

    private final ProcessoCustaPort processoPort;
    private final CustaJudicialStorePort custaStore;
    private final GruCodigoBarrasGenerator gruGenerator;
    private final PixPayloadGenerator pixGenerator;
    private final CustaIsencaoPolicy isentoPolicy;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;

    public CustaJudicialApplicationService(ProcessoCustaPort processoPort,
                                CustaJudicialStorePort custaStore,
                                @Qualifier("gruCodigoBarrasGenerator") GruCodigoBarrasGenerator gruGenerator,
                                PixPayloadGenerator pixGenerator,
                                CustaIsencaoPolicy isentoPolicy,
                                AuditLedgerService auditLedger,
                                ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy) {
        this.processoPort = Objects.requireNonNull(processoPort);
        this.custaStore = Objects.requireNonNull(custaStore);
        this.gruGenerator = Objects.requireNonNull(gruGenerator);
        this.pixGenerator = Objects.requireNonNull(pixGenerator);
        this.isentoPolicy = Objects.requireNonNull(isentoPolicy);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
    }

    @Transactional
    public CustaJudicialResult gerarCustas(GerarCustaJudicialCommand command) {
        Objects.requireNonNull(command);
        return gerarCustas(command.processoId(), command.tipoCusta(), command.valor());
    }

    @Transactional
    public CustaJudicialResult gerarCustas(Long processoId, TipoCusta tipoCusta, BigDecimal valor) {
        Objects.requireNonNull(tipoCusta, "tipoCusta");
        ProcessoCustaContexto contexto = processoPort.obterContexto(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult isencao = isentoPolicy.verificar(contexto.ramoDireito(), contexto.rito(), tipoCusta);
        if (isencao.isento()) {
            CustaJudicial entity = CustaJudicial.isento(processoId, tipoCusta, valor, isencao.motivo());
            custaStore.save(entity);
            readAfterWriteConsistencyPolicy.markWrite();
            auditLedger.appendSafely("CUSTA_ISENCAO", "PROCESSO", String.valueOf(processoId),
                    "tipo=" + tipoCusta.name() + " fundamento=" + tipoCusta.fundamentoLegal() + " motivo=" + isencao.motivo());
            return CustaJudicialResult.isento(entity.getId(), isencao.motivo());
        }
        String uf = contexto.uf() != null && !contexto.uf().isBlank() ? contexto.uf().trim().toUpperCase() : "DF";
        GruResult gru = gruGenerator.gerar(tipoCusta.name(), valor, uf);
        PixResult pix = pixGenerator.gerar(valor, processoId, tipoCusta.name());
        LocalDate vencimento = LocalDate.now().plusDays(30);
        CustaJudicial entity = CustaJudicial.builder()
                .processoId(processoId)
                .tipo(tipoCusta)
                .valor(valor)
                .codigoReceita(gru.codigoReceita())
                .competenciaUf(uf)
                .linhaDigitavel(gru.linhaDigitavel())
                .codigoBarras(gru.codigoBarras())
                .pixPayload(pix.payload())
                .pixTxid(pix.txid())
                .nossoNumero(gru.nossoNumero())
                .status("PENDENTE")
                .vencimento(vencimento)
                .createdAt(Instant.now())
                .build();
        custaStore.save(entity);
        readAfterWriteConsistencyPolicy.markWrite();
        auditLedger.appendSafely("CUSTA_GERADA", "PROCESSO", String.valueOf(processoId),
                "tipo=" + tipoCusta.name() + " fundamento=" + tipoCusta.fundamentoLegal() + " valor=" + valor + " vencimento=" + vencimento);
        return CustaJudicialResult.pendente(entity.getId(), gru, pix, vencimento);
    }

    @Transactional
    public RegistrarPagamentoCustaResult registrarPagamento(RegistrarPagamentoCustaCommand command) {
        Objects.requireNonNull(command);
        CustaJudicial entity = custaStore.findById(command.custaId())
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + command.custaId()));
        entity.setPagoEm(command.pagoEm());
        entity.setValorPago(command.valorPago());
        entity.setStatus("PAGO");
        custaStore.save(entity);
        readAfterWriteConsistencyPolicy.markWrite();
        return new RegistrarPagamentoCustaResult(entity.getId(), entity.getStatus(), true);
    }


    @Transactional(readOnly = true)
    public CustaJudicialView view(Long custaId) {
        CustaJudicial entity = custaStore.findById(custaId)
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
        return new CustaJudicialView(entity.getId(), entity.getTipo() == null ? null : entity.getTipo().name(), entity.getStatus(), entity.getLinhaDigitavel(), entity.getPixPayload(), entity.getVencimento());
    }

    @Transactional(readOnly = true)
    public PixCobrancaView pixView(Long custaId) {
        CustaJudicial entity = custaStore.findById(custaId)
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
        return new PixCobrancaView(entity.getPixTxid(), entity.getPixPayload(), "PENDENTE".equals(entity.getStatus()));
    }


    @Transactional(readOnly = true)
    public CustaConsultaResult consultar(CustaConsultaCommand command) {
        Objects.requireNonNull(command);
        CustaJudicial entity = custaStore.findById(command.custaId())
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + command.custaId()));
        return toConsultaResult(entity);
    }

    @Transactional(readOnly = true)
    public java.util.List<CustaConsultaResult> listarPorProcesso(Long processoId) {
        Objects.requireNonNull(processoId);
        return custaStore.findByProcessoId(processoId).stream()
                .map(this::toConsultaResult)
                .toList();
    }

    private CustaConsultaResult toConsultaResult(CustaJudicial entity) {
        return new CustaConsultaResult(entity.getId(), entity.getTipo() == null ? null : entity.getTipo().name(), entity.getValor(), entity.getStatus(), entity.getVencimento(), entity.getPagoEm(), entity.getValorPago());
    }

    @Transactional(readOnly = true)
    public GruEmissaoSnapshot gruSnapshot(Long custaId) {
        CustaJudicial entity = custaStore.findById(custaId)
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
        return new GruEmissaoSnapshot(entity.getCodigoReceita(), entity.getLinhaDigitavel(), entity.getCodigoBarras(), entity.getNossoNumero());
    }

    @Transactional(readOnly = true)
    public PixPayloadSnapshot pixPayloadSnapshot(Long custaId) {
        CustaJudicial entity = custaStore.findById(custaId)
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
        return new PixPayloadSnapshot(entity.getPixTxid(), entity.getPixPayload(), "PENDENTE".equals(entity.getStatus()));
    }

    public CustaPagamentoCommandSnapshot paymentCommandSnapshot(RegistrarPagamentoCustaCommand command) {
        Objects.requireNonNull(command);
        return new CustaPagamentoCommandSnapshot(command.custaId(), command.valorPago(), command.pagoEm());
    }

    @Transactional(readOnly = true)
    public CustaPagamentoAuditSnapshot paymentAuditSnapshot(Long custaId) {
        CustaJudicial entity = custaStore.findById(custaId)
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
        return new CustaPagamentoAuditSnapshot(entity.getId(), entity.getStatus(), entity.getValorPago(), entity.getPagoEm());
    }


    @Transactional(readOnly = true)
    public CustaConsultaTimelineResult consultarTimeline(CustaConsultaTimelineCommand command) {
        Objects.requireNonNull(command);
        CustaJudicial entity = custaStore.findById(command.custaId())
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + command.custaId()));
        java.util.ArrayList<CustaTimelineEntry> timeline = new java.util.ArrayList<>();
        timeline.add(new CustaTimelineEntry("CUSTA_EMITIDA", entity.getCreatedAt(), entity.getTipo() == null ? null : entity.getTipo().name()));
        timeline.add(new CustaTimelineEntry("VENCIMENTO", entity.getVencimento() == null ? null : entity.getVencimento().atStartOfDay().toInstant(java.time.ZoneOffset.UTC), entity.getStatus()));
        if (entity.getPagoEm() != null) {
            timeline.add(new CustaTimelineEntry("PAGAMENTO_REGISTRADO", entity.getPagoEm(), entity.getValorPago() == null ? null : entity.getValorPago().toPlainString()));
        }
        return new CustaConsultaTimelineResult(entity.getId(), java.util.List.copyOf(timeline));
    }

    @Transactional(readOnly = true)
    public CustaStatusSnapshot statusSnapshot(Long custaId) {
        CustaJudicial entity = custaStore.findById(custaId)
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
        return new CustaStatusSnapshot(entity.getId(), entity.getStatus(), Instant.now());
    }

    @Transactional(readOnly = true)
    public CustaVencimentoSnapshot vencimentoSnapshot(Long custaId) {
        CustaJudicial entity = custaStore.findById(custaId)
                .orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
        LocalDate vencimento = entity.getVencimento();
        return new CustaVencimentoSnapshot(entity.getId(), vencimento, vencimento != null && vencimento.isBefore(LocalDate.now()));
    }



@Transactional(readOnly = true)
public com.tcc.pjb.backend.modules.custas.domain.CustaHealthResult health(com.tcc.pjb.backend.modules.custas.domain.CustaHealthQuery query) {
    java.util.Objects.requireNonNull(query);
    return new com.tcc.pjb.backend.modules.custas.domain.CustaHealthResult(statusSnapshot(query.custaId()), vencimentoSnapshot(query.custaId()));
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.modules.custas.domain.CustaPagamentoView pagamentoView(Long custaId) {
    var entity = custaStore.findById(custaId).orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
    return new com.tcc.pjb.backend.modules.custas.domain.CustaPagamentoView(entity.getId(), entity.getValorPago(), entity.getPagoEm(), entity.getStatus());
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.modules.custas.domain.GruLinhaDigitavelView linhaDigitavelView(Long custaId) {
    var entity = custaStore.findById(custaId).orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
    return new com.tcc.pjb.backend.modules.custas.domain.GruLinhaDigitavelView(entity.getId(), entity.getLinhaDigitavel(), entity.getCodigoBarras());
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.modules.custas.domain.PixCobrancaHealthSnapshot pixHealth(Long custaId) {
    var entity = custaStore.findById(custaId).orElseThrow(() -> new IllegalArgumentException("Custa não encontrada: " + custaId));
    return new com.tcc.pjb.backend.modules.custas.domain.PixCobrancaHealthSnapshot(entity.getId(), entity.getPixTxid(), "PENDENTE".equalsIgnoreCase(entity.getStatus()));
}
}
