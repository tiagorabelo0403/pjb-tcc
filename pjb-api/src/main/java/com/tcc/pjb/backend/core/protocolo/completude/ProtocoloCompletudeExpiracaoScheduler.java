package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloPendenciaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "pjb.jobs.protocolo-completude", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ProtocoloCompletudeExpiracaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProtocoloCompletudeExpiracaoScheduler.class);

    private final ProtocoloPendenciaRepository pendenciaRepository;
    private final ProtocoloPendenciaApplicationService pendenciaService;

    public ProtocoloCompletudeExpiracaoScheduler(ProtocoloPendenciaRepository pendenciaRepository,
                                                  ProtocoloPendenciaApplicationService pendenciaService) {
        this.pendenciaRepository = Objects.requireNonNull(pendenciaRepository);
        this.pendenciaService = Objects.requireNonNull(pendenciaService);
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "America/Sao_Paulo")
    @Transactional
    public void processarExpiradas() {
        List<ProtocoloPendencia> pendentes = pendenciaRepository.findByStatus(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO);
        LocalDate hoje = LocalDate.now();
        int escalados = 0;
        for (ProtocoloPendencia p : pendentes) {
            if (p.getPrazoRegularizacao() != null && p.getPrazoRegularizacao().isBefore(hoje)) {
                pendenciaService.escalarExpirada(p.getId());
                escalados++;
            }
        }
        if (escalados > 0) {
            log.info("Completude scheduler: {} pendencias expiradas escaladas em {}", escalados, hoje);
        }
    }
}
