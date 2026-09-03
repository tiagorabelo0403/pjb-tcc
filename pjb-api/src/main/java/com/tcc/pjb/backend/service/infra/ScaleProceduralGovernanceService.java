package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRightsCoverageService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationService;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Snapshots de cobertura de direitos processuais, playbook operacional por rito e variação por
 * tribunal. Extraído de {@link ScaleArchitectureService} porque esses 3 colaboradores são
 * usados exclusivamente por esse subconjunto de métodos, sem estado compartilhado com os outros
 * grupos de governança de escala.
 */
@Service
public class ScaleProceduralGovernanceService {

    private final NationalProceduralRightsCoverageService proceduralRightsCoverageService;
    private final NationalProceduralOperationalPlaybookService proceduralOperationalPlaybookService;
    private final NationalProceduralTribunalVariationService proceduralTribunalVariationService;

    public ScaleProceduralGovernanceService(NationalProceduralRightsCoverageService proceduralRightsCoverageService,
                                             NationalProceduralOperationalPlaybookService proceduralOperationalPlaybookService,
                                             NationalProceduralTribunalVariationService proceduralTribunalVariationService) {
        this.proceduralRightsCoverageService = Objects.requireNonNull(proceduralRightsCoverageService);
        this.proceduralOperationalPlaybookService = Objects.requireNonNull(proceduralOperationalPlaybookService);
        this.proceduralTribunalVariationService = Objects.requireNonNull(proceduralTribunalVariationService);
    }

    @Transactional(readOnly = true)
    public Object judicialProceduralCoverageView() {
        return proceduralRightsCoverageService.snapshot();
    }

    @Transactional(readOnly = true)
    public Object judicialProceduralCoverageDetailView(String rito) {
        return proceduralRightsCoverageService.describe(rito);
    }

    @Transactional(readOnly = true)
    public Object judicialProceduralPlaybookView() {
        return proceduralOperationalPlaybookService.snapshot();
    }

    @Transactional(readOnly = true)
    public Object judicialProceduralPlaybookDetailView(String rito) {
        return proceduralOperationalPlaybookService.describe(rito);
    }

    @Transactional(readOnly = true)
    public Object judicialTribunalVariationView() {
        return proceduralTribunalVariationService.snapshot();
    }

    @Transactional(readOnly = true)
    public Object judicialTribunalVariationDetailView(String tribunalCodigo, String rito, String unidadeCodigo, String tipoJustica) {
        return proceduralTribunalVariationService.describe(tribunalCodigo, unidadeCodigo, rito, tipoJustica);
    }
}
