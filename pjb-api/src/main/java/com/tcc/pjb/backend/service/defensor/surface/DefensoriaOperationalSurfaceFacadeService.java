package com.tcc.pjb.backend.service.defensor.surface;

import com.tcc.pjb.backend.model.dto.defensor.surface.DefensoriaVulnerabilidadePriorizarRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.defensor.DefensoriaPublicaOperacionalService;
import com.tcc.pjb.backend.service.defensor.DefensoriaVulnerabilidadeService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class DefensoriaOperationalSurfaceFacadeService {

    private final DefensoriaPublicaOperacionalService operacionalService;
    private final DefensoriaVulnerabilidadeService vulnerabilidadeService;
    private final SurfaceProjectionSupport projectionSupport;

    public DefensoriaOperationalSurfaceFacadeService(DefensoriaPublicaOperacionalService operacionalService,
                                                     DefensoriaVulnerabilidadeService vulnerabilidadeService,
                                                     SurfaceProjectionSupport projectionSupport) {
        this.operacionalService = operacionalService;
        this.vulnerabilidadeService = vulnerabilidadeService;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse snapshot() {
        return projectionSupport.snapshot("defensoria.operacional", operacionalService.bootstrapPainel());
    }

    public SurfaceActionResponse apresentarDefesa(Long processoId, String defesa, String fundamentacao) {
        return projectionSupport.action("defensoria.operacional", "apresentarDefesa", processoId,
                operacionalService.apresentarDefesa(processoId, defesa, fundamentacao));
    }

    public SurfaceActionResponse impetrarHabeasCorpus(Long processoId, String impetrante, String paciente, String fundamentacao) {
        return projectionSupport.action("defensoria.operacional", "impetrarHabeasCorpus", processoId,
                operacionalService.impetrarHabeasCorpus(processoId, impetrante, paciente, fundamentacao));
    }


    public SurfaceSnapshotResponse malhaProcesso(Long processoId) {
        return projectionSupport.snapshot("defensoria.malhaProcesso", operacionalService.malhaProcesso(processoId));
    }
    public SurfaceActionResponse solicitarAssistenciaGratuita(Long processoId, String renda, String justificativa) {
        String rendaValue = null;
        if (renda != null && !renda.isBlank()) {
            String normalized = renda.replace(".", "").replace(",", ".").replaceAll("[^0-9.-]", "");
            if (!normalized.isBlank()) {
                rendaValue = String.valueOf(Double.parseDouble(normalized));
            }
        }
        return projectionSupport.action("defensoria.operacional", "solicitarAssistenciaGratuita", processoId,
                operacionalService.solicitarAssistenciaJudiciariaGratuita(processoId, rendaValue, justificativa));
    }

    public SurfaceCollectionResponse listarAssistidosSemAdvogado() {
        return projectionSupport.collection("defensoria.assistidosSemAdvogado", operacionalService.listarAssistidosSemAdvogado());
    }

    public SurfaceSnapshotResponse priorizar(DefensoriaVulnerabilidadePriorizarRequest request) {
        DefensoriaVulnerabilidadeService.PriorizarCasoRequest mapped = new DefensoriaVulnerabilidadeService.PriorizarCasoRequest(
                request.processoId(),
                request.assistidoNome(),
                request.documentoIdentificador(),
                request.criancaOuAdolescente(),
                request.idoso(),
                request.pessoaComDeficiencia(),
                request.violenciaDomestica(),
                request.privacaoLiberdade(),
                request.saudeGrave(),
                request.semRendaOuRua(),
                request.mulherChefeFamilia(),
                request.riscoAlimentar(),
                request.observacoes()
        );
        return projectionSupport.snapshot("defensoria.vulnerabilidade", vulnerabilidadeService.priorizar(mapped));
    }

    public SurfaceCollectionResponse listarCasos(String status) {
        return projectionSupport.collection("defensoria.vulnerabilidade", vulnerabilidadeService.listar(status));
    }
}
