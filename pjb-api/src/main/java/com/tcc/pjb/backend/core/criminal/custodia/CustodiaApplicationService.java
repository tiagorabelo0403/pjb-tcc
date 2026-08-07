package com.tcc.pjb.backend.core.criminal.custodia;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPrazoConsultaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.RegistrarPrisaoCommand;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustodiaApplicationService {

    private final AudienciaCustodiaService audienciaCustodiaService;
    private final AuditLedgerService auditLedgerService;

    public CustodiaApplicationService(AudienciaCustodiaService audienciaCustodiaService,
                                      AuditLedgerService auditLedgerService) {
        this.audienciaCustodiaService = Objects.requireNonNull(audienciaCustodiaService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public com.tcc.pjb.backend.core.criminal.custodia.domain.AudienciaCustodiaResult registrarPrisao(Long processoId,
                                                                                                       String presoNome,
                                                                                                       String presoCpf,
                                                                                                       Instant dataPrisao) {
        var result = audienciaCustodiaService.registrarPrisao(new RegistrarPrisaoCommand(processoId, presoNome, presoCpf, dataPrisao));
        auditLedgerService.appendSafely("CUSTODIA_PRISAO_MANUAL", "PROCESSO", String.valueOf(processoId), null, presoNome);
        return result;
    }

    @Transactional
    public com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCustodiaResult concluir(Long custodiaId,
                                                                                                        String resultado,
                                                                                                        List<String> medidasCautelares) {
        var result = audienciaCustodiaService.concluirAudienciaResumo(new ConcluirAudienciaCommand(custodiaId, resultado, medidasCautelares));
        auditLedgerService.appendSafely("CUSTODIA_CONCLUSAO_MANUAL", "CUSTODIA", String.valueOf(custodiaId), null, resultado);
        return result;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaResult consulta(Long custodiaId) {
        return audienciaCustodiaService.consultar(new CustodiaConsultaCommand(requireId(custodiaId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPrazoConsultaResult prazo(Long custodiaId) {
        return audienciaCustodiaService.consultarPrazo(new CustodiaPrazoConsultaCommand(requireId(custodiaId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineResult timeline(Long custodiaId) {
        Long requiredId = requireId(custodiaId);
        var result = audienciaCustodiaService.consultarTimeline(new CustodiaConsultaTimelineCommand(requiredId));
        auditLedgerService.appendSafely("CUSTODIA_TIMELINE_QUERY", "CUSTODIA", String.valueOf(requiredId), null, "entries=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaResultadoSnapshot resultado(Long custodiaId) {
        return audienciaCustodiaService.resultadoSnapshot(requireId(custodiaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaAuditoriaSnapshot auditoria(Long custodiaId) {
        return audienciaCustodiaService.auditoria(requireId(custodiaId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaAndamentoSnapshot andamento(Long custodiaId) {
        return audienciaCustodiaService.andamento(requireId(custodiaId));
    }

    @Transactional(readOnly = true)
    public java.util.List<com.tcc.pjb.backend.core.criminal.custodia.domain.MedidaCautelarSnapshot> medidas(Long processoId) {
        return audienciaCustodiaService.medidas(processoId);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPendenteView> pendentes() {
        return audienciaCustodiaService.pendentes();
    }

    private Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("custodiaId obrigatorio");
        }
        return id;
    }
}
