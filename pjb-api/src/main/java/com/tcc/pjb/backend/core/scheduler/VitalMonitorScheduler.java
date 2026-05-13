package com.tcc.pjb.backend.core.scheduler;

import java.util.EnumSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.integration.govt.CrcIntegrationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pjb.gov.vital-monitor.enabled", havingValue = "true")
public class VitalMonitorScheduler {

    private final ProcessoRepository processoRepository;
    private final CrcIntegrationService crcIntegrationService;

    
    @Scheduled(fixedDelayString = "${pjb.gov.vital-monitor.delay-ms:900000}")
    public void monitorar() {
        try {
            int restante = getMaxPerCycle();
            int page = 0;
            int analisados = 0;
            while (restante > 0) {
                Slice<Processo> slice = processoRepository.findAllForVitalMonitor(
                        EnumSet.of(StatusProcesso.JULGADO, StatusProcesso.ARQUIVADO, StatusProcesso.TRANSITO_EM_JULGADO),
                        PageRequest.of(page, Math.min(restante, pageSize()))
                );
                if (slice.isEmpty()) {
                    break;
                }
                for (Processo processo : slice.getContent()) {
                    if (processo == null || processo.getUsuario() == null || processo.getUsuario().getCpf() == null) {
                        continue;
                    }
                    crcIntegrationService.verificarProcesso(processo, "system");
                    analisados++;
                    restante--;
                    if (restante <= 0) {
                        break;
                    }
                }
                if (!slice.hasNext()) {
                    break;
                }
                page++;
            }
            log.info("VitalMonitorScheduler ciclo concluído: analisados={}", analisados);
        } catch (Exception e) {
            log.warn("VitalMonitorScheduler falhou: {}", e.getMessage());
        }
    }

    private int getMaxPerCycle() {
        return 200;
    }

    private int pageSize() {
        return 64;
    }
}
