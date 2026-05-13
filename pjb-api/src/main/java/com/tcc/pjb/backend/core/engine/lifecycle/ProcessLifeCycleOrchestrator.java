package com.tcc.pjb.backend.core.engine.lifecycle;

import java.time.Instant;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.engine.financial.SmartPaymentEngine;
import com.tcc.pjb.backend.core.infra.cache.MortalityCache;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.AuditoriaService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessLifeCycleOrchestrator {

    private final ProcessoRepository processoRepository;
    private final MortalityCache mortalityCache;
    private final AuditoriaService auditoriaService;
    private final SmartPaymentEngine smartPaymentEngine;
    private final UiHistoryService uiHistoryService;

    
    public void onDeathDetected(Long processoId, String cpf, MortalityCache.VitalStatus status, String operador) {
        try {
            if (cpf == null || cpf.isBlank() || processoId == null) return;

            
            MortalityCache.VitalStatus deceased = MortalityCache.VitalStatus.builder()
                    .state(MortalityCache.VitalStatus.State.DECEASED)
                    .source(status != null ? status.getSource() : "unknown")
                    .reference(status != null ? status.getReference() : "no_ref")
                    .checkedAt(Instant.now())
                    .deathDate(status != null ? status.getDeathDate() : null)
                    .build();
            mortalityCache.put(cpf, deceased);

            
            Processo p = processoRepository.findById(processoId).orElse(null);
            if (p != null) {
                if (p.getStatusProcesso() == null || p.getStatusProcesso().isAtivo()) {
                    StatusProcesso fromStatus = p.getStatusProcesso();
                    String fromResultado = p.getResultadoFinal();

                    p.setStatusProcesso(StatusProcesso.SUSPENSO_POR_OBITO);
                    p.setDataUltimaMovimentacao(java.time.LocalDateTime.now());
                    Processo saved = processoRepository.save(p);

                    uiHistoryService.recordProcessoStatusChange(
                        saved,
                        fromStatus,
                        fromResultado,
                        StatusProcesso.SUSPENSO_POR_OBITO,
                        fromResultado,
                        "Processo suspenso por óbito (operador=" + (operador == null ? "system" : operador) + ")"
                    );
                }
            }

            
            smartPaymentEngine.flagDeceased(cpf, deceased);

            
            auditoriaService.registrar(operador == null ? "system" : operador,
                    "OBITO_DETECTADO",
                    String.valueOf(processoId));

            log.warn("[LIFECYCLE] Obito detectado -> processo={} cpf={} source={}", processoId, cpf, deceased.getSource());
        } catch (Exception e) {
            log.warn("[LIFECYCLE] Falha ao aplicar protocolo de obito: {}", e.getMessage());
        }
    }
}
