package com.tcc.pjb.backend.service.intelligence.surface;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.intelligence.TetoSalarioMinimoResponse;
import com.tcc.pjb.backend.model.dto.radar.RadarPadroesRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.teto.SalarioMinimoUpsertRequest;
import com.tcc.pjb.backend.model.dto.teto.TetoProcessualDiagnosticoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class IntelligenceOperationalSurfaceFacadeService {

    private final RadarPadroesService radarPadroesService;
    private final TetoProcessualService tetoProcessualService;
    private final SalarioMinimoNacionalService salarioMinimoNacionalService;
    private final ProcessoRepository processoRepository;
    private final SurfaceProjectionSupport surfaceProjectionSupport;

    public IntelligenceOperationalSurfaceFacadeService(RadarPadroesService radarPadroesService,
                                                       TetoProcessualService tetoProcessualService,
                                                       SalarioMinimoNacionalService salarioMinimoNacionalService,
                                                       ProcessoRepository processoRepository,
                                                       SurfaceProjectionSupport surfaceProjectionSupport) {
        this.radarPadroesService = Objects.requireNonNull(radarPadroesService);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.surfaceProjectionSupport = Objects.requireNonNull(surfaceProjectionSupport);
    }

    public SurfaceSnapshotResponse analisarRadar(RadarPadroesRequest request) {
        RadarPadroesService.ContextoRadar contexto = new RadarPadroesService.ContextoRadar(
                request.processoId(),
                request.nupn(),
                request.documentoAutor(),
                request.documentoReu(),
                request.escritorioOab(),
                request.tribunalCodigo(),
                request.ramoDireito(),
                request.classeProcessual(),
                request.assunto(),
                request.valorCausa(),
                request.resumoFatos(),
                request.dataAjuizamento(),
                request.statusProcesso(),
                request.resultadoFinal(),
                null
        );
        return surfaceProjectionSupport.snapshot("intelligence.radar.analise", radarPadroesService.analisarERegistrar(contexto));
    }

    public SurfaceSnapshotResponse analisarRadarProcesso(Long processoId) {
        return surfaceProjectionSupport.snapshot("intelligence.radar.processo", radarPadroesService.analisarERegistrarProcesso(processoId));
    }

    public SurfaceSnapshotResponse ultimoRadarPorProcesso(Long processoId) {
        return radarPadroesService.buscarUltimoPorProcesso(processoId)
                .map(result -> surfaceProjectionSupport.snapshot("intelligence.radar.latest", result))
                .orElse(null);
    }

    public SurfaceCollectionResponse alertasRadarPorProcesso(Long processoId) {
        return surfaceProjectionSupport.collection("intelligence.radar.alertas.processo", radarPadroesService.alertasDoProcesso(processoId));
    }

    public SurfaceCollectionResponse alertasRadarPorNupn(String nupn) {
        return surfaceProjectionSupport.collection("intelligence.radar.alertas.nupn", radarPadroesService.alertasPorNupn(nupn));
    }

    public SurfaceSnapshotResponse diagnosticarTetoProcesso(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new NoSuchElementException("Processo nao encontrado: " + processoId));
        return surfaceProjectionSupport.snapshot("intelligence.teto.processo", tetoProcessualService.diagnosticar(processo));
    }

    public SurfaceSnapshotResponse diagnosticarTeto(TetoProcessualDiagnosticoRequest request) {
        return surfaceProjectionSupport.snapshot(
                "intelligence.teto.diagnostico",
                tetoProcessualService.diagnosticar(new TetoProcessualService.ContextoTetoProcessual(
                        request.valorCausa(),
                        TipoJustica.fromString(request.tipoJustica()),
                        RamoDireito.fromString(request.ramoDireito()),
                        RitoProcessual.tryParse(request.ritoProcessual()).orElse(null),
                        null,
                        request.dataReferencia(),
                        request.classeProcessual(),
                        request.assunto(),
                        null
                ))
        );
    }

    public SurfaceCollectionResponse listarSalariosMinimos() {
        List<TetoSalarioMinimoResponse> responses = salarioMinimoNacionalService.listarAtivos().stream()
                .map(this::mapSalario)
                .toList();
        return surfaceProjectionSupport.collection("intelligence.teto.salario-minimo", responses);
    }

    public SurfaceSnapshotResponse salvarSalarioMinimo(SalarioMinimoUpsertRequest request) {
        SalarioMinimoNacional saved = salarioMinimoNacionalService.salvarOuAtualizar(
                request.anoReferencia(),
                request.valorMensal(),
                request.normaReferencia(),
                request.fonteOficial()
        );
        return surfaceProjectionSupport.snapshot("intelligence.teto.salario-minimo.save", mapSalario(saved));
    }

    private TetoSalarioMinimoResponse mapSalario(SalarioMinimoNacional salario) {
        return new TetoSalarioMinimoResponse(
                salario.getId(),
                salario.getAnoReferencia(),
                salario.getValorMensal(),
                salario.getNormaReferencia(),
                salario.getFonteOficial(),
                salario.getAtivo()
        );
    }
}
