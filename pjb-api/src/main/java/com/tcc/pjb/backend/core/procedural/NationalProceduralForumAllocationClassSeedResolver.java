package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumAllocationClassSeedResolver {

    private final ProceduralCatalogService proceduralCatalogService;

    public NationalProceduralForumAllocationClassSeedResolver(ProceduralCatalogService proceduralCatalogService) {
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
    }

    NationalProceduralForumAllocationClassSeed resolve(NationalProceduralForumAllocationContext context) {
        Objects.requireNonNull(context);
        Map<String, Object> payload = context.payload();
        RitoProcessual rito = RitoProcessual.tryParse(context.ritoSugerido()).orElse(context.canonical().rito());
        Optional<TpuClasseCnj> classeTpu = proceduralCatalogService.resolveClasseTpu(
                NationalProceduralRoutingSupport.firstNonBlank(
                        NationalProceduralRoutingSupport.text(payload.get("classeTpu")),
                        NationalProceduralRoutingSupport.text(payload.get("classe")),
                        NationalProceduralRoutingSupport.text(payload.get("classeProcessual")),
                        context.canonical().classeTpuCodigo(),
                        context.canonical().classeTpuNome()
                ),
                rito
        );
        return new NationalProceduralForumAllocationClassSeed(rito, classeTpu.orElse(null));
    }
}
