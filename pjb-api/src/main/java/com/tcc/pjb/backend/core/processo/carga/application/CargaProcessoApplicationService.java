package com.tcc.pjb.backend.core.processo.carga.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.processo.CargaProcesso;
import com.tcc.pjb.backend.model.repository.CargaProcessoRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CargaProcessoApplicationService {

    private final CargaProcessoRepository cargaRepository;
    private final AuditLedgerService auditLedgerService;

    @Inject
    public CargaProcessoApplicationService(CargaProcessoRepository cargaRepository,
                                            AuditLedgerService auditLedgerService) {
        this.cargaRepository = cargaRepository;
        this.auditLedgerService = auditLedgerService;
    }

    @Transactional
    public CargaProcesso registrarSaida(Long processoId, Long responsavelId, Long unidadeId,
                                         String tipoCarga, Instant dataPrevista,
                                         int volumes, String observacao) {
        cargaRepository.findCargaAtivaByProcessoId(processoId)
                .ifPresent(c -> {
                    throw new IllegalStateException(
                            "Já existe carga ativa para o processo: " + processoId);
                });
        CargaProcesso carga = new CargaProcesso(processoId, responsavelId, unidadeId,
                tipoCarga, dataPrevista, volumes, observacao);
        return cargaRepository.save(carga);
    }

    @Transactional
    public CargaProcesso registrarRetorno(Long cargaId, Long operadorId) {
        CargaProcesso carga = cargaRepository.findById(cargaId)
                .orElseThrow(() -> new EntityNotFoundException("Carga não encontrada: " + cargaId));
        if (!carga.isAtiva()) {
            throw new IllegalStateException("Carga não está ativa: " + carga.getStatus());
        }
        carga.registrarRetorno();
        return cargaRepository.save(carga);
    }

    @Transactional
    public CargaProcesso registrarExtravio(Long cargaId, Long operadorId, String observacao) {
        CargaProcesso carga = cargaRepository.findById(cargaId)
                .orElseThrow(() -> new EntityNotFoundException("Carga não encontrada: " + cargaId));
        carga.registrarExtravio(observacao);
        cargaRepository.save(carga);
        auditLedgerService.appendSafely("CARGA_PROCESSO_EXTRAVIADA",
                "CargaProcesso", String.valueOf(cargaId), null);
        return carga;
    }

    @Transactional(readOnly = true)
    public Optional<CargaProcesso> cargaAtiva(Long processoId) {
        return cargaRepository.findCargaAtivaByProcessoId(processoId);
    }

    @Transactional(readOnly = true)
    public List<CargaProcesso> historico(Long processoId) {
        return cargaRepository.findByProcessoIdOrderByDataSaidaAsc(processoId);
    }
}
