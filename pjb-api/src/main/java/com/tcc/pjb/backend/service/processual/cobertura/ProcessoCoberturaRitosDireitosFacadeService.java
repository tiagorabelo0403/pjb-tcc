package com.tcc.pjb.backend.service.processual.cobertura;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRightsCoverageFamily;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRightsCoverageRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRightsCoverageService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRightsCoverageSnapshot;
import com.tcc.pjb.backend.model.dto.processual.cobertura.ProcessoProceduralCoverageFamilyResponse;
import com.tcc.pjb.backend.model.dto.processual.cobertura.ProcessoProceduralCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.cobertura.ProcessoProceduralGuaranteeResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoCoberturaRitosDireitosFacadeService {

    private final NationalProceduralRightsCoverageService coverageService;

    public ProcessoCoberturaRitosDireitosFacadeService(NationalProceduralRightsCoverageService coverageService) {
        this.coverageService = Objects.requireNonNull(coverageService);
    }

    @Transactional(readOnly = true)
    public ProcessoProceduralCoverageResponse coberturaCompleta() {
        NationalProceduralRightsCoverageSnapshot snapshot = coverageService.snapshot();
        return new ProcessoProceduralCoverageResponse(
                snapshot.generatedAt(),
                snapshot.supportsAllBrazilianRites(),
                snapshot.supportsAllBrazilianRights(),
                snapshot.supportsAllProceduralGuarantees(),
                snapshot.totalRitos(),
                snapshot.totalRamos(),
                snapshot.totalGrupos(),
                snapshot.justiceTracks(),
                snapshot.constitutionalGuarantees(),
                snapshot.familyCoverage().stream().map(this::toFamily).toList(),
                snapshot.ritoCoverage().stream().map(this::toGuarantee).toList(),
                snapshot.metadata()
        );
    }

    @Transactional(readOnly = true)
    public ProcessoProceduralGuaranteeResponse detalhar(String rito) {
        return toGuarantee(coverageService.describe(rito));
    }

    private ProcessoProceduralCoverageFamilyResponse toFamily(NationalProceduralRightsCoverageFamily family) {
        return new ProcessoProceduralCoverageFamilyResponse(
                family.familyCode(),
                family.displayName(),
                family.totalRitos(),
                family.segredoPadrao(),
                family.exigeMinisterioPublico(),
                family.admiteConciliacao(),
                family.justiceTracks(),
                family.markers()
        );
    }

    private ProcessoProceduralGuaranteeResponse toGuarantee(NationalProceduralRightsCoverageRow row) {
        return new ProcessoProceduralGuaranteeResponse(
                row.rito(),
                row.ramo(),
                row.grupo(),
                row.protocoloSugerido(),
                row.segredoPadrao(),
                row.exigeMinisterioPublico(),
                row.admiteConciliacao(),
                row.admiteJuizado(),
                row.autocompositivo(),
                row.internacional(),
                row.coletivoOuEstrutural(),
                row.justiceTracks(),
                row.garantiasEssenciais(),
                row.checkpointsOperacionais(),
                row.marcadores()
        );
    }
}
