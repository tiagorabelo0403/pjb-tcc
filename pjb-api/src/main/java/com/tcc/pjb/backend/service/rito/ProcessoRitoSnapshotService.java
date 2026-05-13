package com.tcc.pjb.backend.service.rito;

import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoRitoSnapshotService {

    private final RitoResolutionService ritoResolutionService;

    public ProcessoRitoSnapshotService(RitoResolutionService ritoResolutionService) {
        this.ritoResolutionService = Objects.requireNonNull(ritoResolutionService);
    }

    public ProcessoRitoSnapshot resolve(Processo processo, String lastMovText) {
        RitoResolutionService.RitoResolutionDetail detail = ritoResolutionService.resolveDetailed(processo, lastMovText);
        RitoResolutionService.RitoResolution resolution = detail != null ? detail.resolution() : null;
        RitoProcessual rito = resolution != null ? resolution.rito() : null;
        if (rito == null && processo != null) {
            rito = processo.getRito();
        }
        if (rito == null && processo != null) {
            rito = ProceduralRitoNames.parse(processo.getClasseProcessual());
        }
        String ritoCode = rito != null ? rito.name() : null;
        String ritoTitle = firstNonBlank(resolution != null ? resolution.ritoTitle() : null, defaultTitle(rito));
        String ramo = firstNonBlank(resolution != null ? resolution.ramoSugerido() : null, null);
        Double confidence = resolution != null ? resolution.confidence() : null;
        boolean blocking = detail != null && detail.blocking();
        boolean needsReview = blocking || (confidence != null && confidence.doubleValue() < 0.70d);
        List<String> reasons = resolution != null && resolution.reasons() != null ? List.copyOf(resolution.reasons()) : List.of();
        String status = detail != null ? detail.status() : null;
        return new ProcessoRitoSnapshot(rito, ritoCode, ritoTitle, ramo, confidence, needsReview, reasons, status, blocking);
    }

    private static String defaultTitle(RitoProcessual rito) {
        if (rito == null) {
            return null;
        }
        return switch (rito) {
            case COMUM_ORDINARIO -> "Rito Comum";
            case TRABALHISTA_SUMARISSIMO -> "Trabalhista (Sumaríssimo)";
            case TRABALHISTA_SUMARIO_ALCADA -> "Trabalhista (Sumário de Alçada)";
            case TRABALHISTA_ORDINARIO -> "Trabalhista (Ordinário)";
            case TRABALHISTA_INQUERITO_FALTA_GRAVE -> "Trabalhista (Inquérito de Falta Grave)";
            case TRABALHISTA_ACAO_CUMPRIMENTO -> "Trabalhista (Ação de Cumprimento)";
            case ADMINISTRATIVO_PAD -> "Administrativo (PAD)";
            case ADMINISTRATIVO_CONCURSO_PUBLICO -> "Administrativo (Concurso Público)";
            case ADMINISTRATIVO_SERVIDORES -> "Administrativo (Servidores)";
            case INFANCIA_JUVENTUDE_ECA -> "Infância e Juventude (Proteção ECA)";
            case INFANCIA_JUVENTUDE_ADOCAO -> "Infância e Juventude (Adoção)";
            case INFANCIA_JUVENTUDE_INFRACIONAL -> "Infância e Juventude (Ato Infracional)";
            case INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR -> "Infância e Juventude (Tutela/Curatela de Menor)";
            case JUIZADO_ESPECIAL_CIVEL -> "Juizado Especial Cível";
            default -> rito.name();
        };
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback != null && !fallback.isBlank() ? fallback : null;
    }

    public record ProcessoRitoSnapshot(
            RitoProcessual rito,
            String ritoCode,
            String ritoTitle,
            String ramo,
            Double confidence,
            boolean needsReview,
            List<String> reasons,
            String status,
            boolean blocking
    ) {
    }
}
