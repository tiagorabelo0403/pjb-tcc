package com.tcc.pjb.backend.service.publico.surface;

import com.tcc.pjb.backend.inovacao.atlas.AtlasAcessoJusticaService;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PublicJusticeAnalyticsSurfaceFacadeService {

    private final PainelNacionalJusticaService painelService;
    private final PerfilInstanciaTribunalService perfilService;
    private final AtlasAcessoJusticaService atlasService;
    private final SurfaceProjectionSupport projectionSupport;

    public PublicJusticeAnalyticsSurfaceFacadeService(PainelNacionalJusticaService painelService,
                                                      PerfilInstanciaTribunalService perfilService,
                                                      AtlasAcessoJusticaService atlasService,
                                                      SurfaceProjectionSupport projectionSupport) {
        this.painelService = painelService;
        this.perfilService = perfilService;
        this.atlasService = atlasService;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse painelSnapshot() {
        return projectionSupport.snapshot("painel-nacional-justica", painelService.gerarSnapshot());
    }

    public java.util.Optional<SurfaceSnapshotResponse> painelTribunal(String codigo) {
        return painelService.metricasTribunal(codigo)
                .map(metricas -> projectionSupport.snapshot("painel-nacional-justica", metricas));
    }

    public SurfaceCollectionResponse painelSerieTemporal(int dias, String tribunalCodigo) {
        return projectionSupport.collection("painel-nacional-justica", painelService.serieTemporal(dias, tribunalCodigo));
    }

    public SurfaceCollectionResponse painelAlertas(String nivel, String tribunalCodigo) {
        PainelNacionalJusticaService.NivelAlerta nivelResolvido = nivel == null || nivel.isBlank()
                ? PainelNacionalJusticaService.NivelAlerta.BAIXO
                : PainelNacionalJusticaService.NivelAlerta.valueOf(nivel.trim().toUpperCase(java.util.Locale.ROOT));
        return projectionSupport.collection("painel-nacional-justica", painelService.alertasPrazo(nivelResolvido, tribunalCodigo));
    }

    public SurfaceSnapshotResponse tribunalPerfilAtivo(String codigo) {
        return projectionSupport.snapshot("tribunal-perfil", codigo != null && !codigo.isBlank()
                ? perfilService.resolverPorCodigoOuPadrao(codigo)
                : perfilService.perfilAtivo().orElseGet(() -> perfilService.resolverPorCodigoOuPadrao("PJB_PADRAO")));
    }

    public SurfaceSnapshotResponse tribunalPerfilPorCodigo(String codigo) {
        return projectionSupport.snapshot("tribunal-perfil", perfilService.resolverPorCodigoOuPadrao(codigo));
    }

    public SurfaceSnapshotResponse tribunalResumo(String codigo) {
        return projectionSupport.snapshot("tribunal-perfil", perfilService.resumo(codigo));
    }

    public SurfaceSnapshotResponse tribunalBindings(String codigo) {
        return projectionSupport.snapshot("tribunal-perfil", perfilService.bindingsDocumento(codigo));
    }

    public SurfaceCollectionResponse tribunalComparar(String codigoA, String codigoB) {
        return projectionSupport.collection("tribunal-perfil", toNamedCollection(perfilService.compararTerminologia(codigoA, codigoB)));
    }

    public SurfaceCollectionResponse tribunalRanking() {
        return projectionSupport.collection("tribunal-perfil", perfilService.rankingPersonalizacao().stream()
                .map(entry -> Map.of("codigo", entry.getKey(), "score", entry.getValue()))
                .toList());
    }

    public SurfaceSnapshotResponse atlasResumoNacional() {
        return projectionSupport.snapshot("atlas-acesso-justica", atlasService.resumoNacional());
    }

    public SurfaceCollectionResponse atlasHeatmap() {
        return projectionSupport.collection("atlas-acesso-justica", atlasService.gerarHeatmapNacional());
    }

    public SurfaceSnapshotResponse atlasRelatorioUf(String uf) {
        return projectionSupport.snapshot("atlas-acesso-justica", atlasService.gerarRelatorioUF(uf));
    }

    public SurfaceCollectionResponse atlasMunicipiosUf(String uf) {
        return projectionSupport.collection("atlas-acesso-justica", atlasService.municipiosPorUf(uf));
    }

    public java.util.Optional<SurfaceSnapshotResponse> atlasMunicipio(String codigoIbge) {
        return atlasService.buscarMunicipio(codigoIbge)
                .map(municipio -> projectionSupport.snapshot("atlas-acesso-justica", municipio));
    }

    public SurfaceCollectionResponse atlasPorClassificacao(String classificacao) {
        AtlasAcessoJusticaService.ClassificacaoDeserto classificacaoResolvida = AtlasAcessoJusticaService.ClassificacaoDeserto.valueOf(classificacao.trim().toUpperCase(java.util.Locale.ROOT));
        return projectionSupport.collection("atlas-acesso-justica", atlasService.municipiosPorClassificacao(classificacaoResolvida));
    }

    private List<Map<String, Object>> toNamedCollection(Map<String, PerfilInstanciaTribunalService.DiferencaTerminologica> source) {
        return source.entrySet().stream()
                .map(entry -> Map.of("campo", entry.getKey(), "diferenca", entry.getValue()))
                .toList();
    }
}
