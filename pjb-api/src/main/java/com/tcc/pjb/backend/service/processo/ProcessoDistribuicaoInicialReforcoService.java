package com.tcc.pjb.backend.service.processo;

import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.distribuicao.ProcessoInitialDistributionSnapshotService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessoDistribuicaoInicialReforcoService {

    private static final Logger log = LoggerFactory.getLogger(ProcessoDistribuicaoInicialReforcoService.class);

    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;
    private final ProcessoInitialDistributionSnapshotService processoInitialDistributionSnapshotService;

    public ProcessoDistribuicaoInicialReforcoService(MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine,
                                                     ProcessoInitialDistributionSnapshotService processoInitialDistributionSnapshotService) {
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
        this.processoInitialDistributionSnapshotService = Objects.requireNonNull(processoInitialDistributionSnapshotService);
    }

    public DynamicCompetenceDistributionResponse ensureSnapshot(Processo processo) {
        if (processo == null) {
            return null;
        }
        boolean missingSnapshot = isBlank(processo.getUnidadeJudiciariaCodigo()) || isBlank(processo.getTribunalCodigoRoteado());
        boolean staleSnapshot = missingSnapshot
                || isBlank(processo.getPreProtocoloStatus())
                || isBlank(processo.getCompetenciaTerritorialModo())
                || isBlank(processo.getPreventionMode())
                || isBlank(processo.getLinkageMode());
        DynamicCompetenceDistributionResponse distribuicao = null;
        try {
            if (missingSnapshot) {
                distribuicao = mapaCompetenciaDinamicoEngine.registrarDistribuicaoInicial(processo).orElse(null);
            }
            if (staleSnapshot) {
                processoInitialDistributionSnapshotService.consolidar(processo);
            }
            return distribuicao;
        } catch (Exception ex) {
            log.warn("Falha nao bloqueante ao reforcar distribuicao inicial. processoId={} erro={}", processo.getId(), ex.getMessage());
            return distribuicao;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
