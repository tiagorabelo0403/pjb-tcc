package com.tcc.pjb.backend.modules.custas.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaCommand;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaTimelineCommand;
import com.tcc.pjb.backend.modules.custas.domain.CustaHealthQuery;
import com.tcc.pjb.backend.modules.custas.domain.GerarCustaJudicialCommand;
import com.tcc.pjb.backend.modules.custas.domain.TipoCusta;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustasApplicationService {

    private final CustaJudicialApplicationService custaJudicialService;
    private final AuditLedgerService auditLedgerService;

    public CustasApplicationService(CustaJudicialApplicationService custaJudicialService,
                                    AuditLedgerService auditLedgerService) {
        this.custaJudicialService = Objects.requireNonNull(custaJudicialService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public com.tcc.pjb.backend.modules.custas.domain.CustaJudicialResult gerar(Long processoId, TipoCusta tipo, BigDecimal valor) {
        var result = custaJudicialService.gerarCustas(new GerarCustaJudicialCommand(processoId, tipo, valor));
        auditLedgerService.appendSafely("CUSTA_GERACAO_MANUAL", "PROCESSO", String.valueOf(processoId), null, "isento=" + result.isento());
        return result;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.CustaConsultaResult consulta(Long custaId) {
        return custaJudicialService.consultar(new CustaConsultaCommand(requireId(custaId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.CustaHealthResult health(Long custaId) {
        return custaJudicialService.health(new CustaHealthQuery(requireId(custaId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.CustaPagamentoView pagamento(Long custaId) {
        return custaJudicialService.pagamentoView(requireId(custaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.GruLinhaDigitavelView linhaDigitavel(Long custaId) {
        return custaJudicialService.linhaDigitavelView(requireId(custaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.PixCobrancaHealthSnapshot pixHealth(Long custaId) {
        return custaJudicialService.pixHealth(requireId(custaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.CustaConsultaTimelineResult timeline(Long custaId) {
        Long requiredId = requireId(custaId);
        var result = custaJudicialService.consultarTimeline(new CustaConsultaTimelineCommand(requiredId));
        auditLedgerService.appendSafely("CUSTA_TIMELINE_QUERY", "CUSTA", String.valueOf(requiredId), null, "entries=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.CustaStatusSnapshot status(Long custaId) {
        return custaJudicialService.statusSnapshot(requireId(custaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.modules.custas.domain.CustaVencimentoSnapshot vencimento(Long custaId) {
        return custaJudicialService.vencimentoSnapshot(requireId(custaId));
    }

    private Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("custaId obrigatorio");
        }
        return id;
    }
}
