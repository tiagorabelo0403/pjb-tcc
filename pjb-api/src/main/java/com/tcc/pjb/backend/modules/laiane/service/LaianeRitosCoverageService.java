package com.tcc.pjb.backend.modules.laiane.service;

import java.util.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeRitosCoverageResponse;
import com.tcc.pjb.backend.service.rito.RitoPackService;

@Service
public class LaianeRitosCoverageService {

    private final RitoPackService ritoPackService;

    public LaianeRitosCoverageService(RitoPackService ritoPackService) {
        this.ritoPackService = ritoPackService;
    }

    @Cacheable(cacheNames = "laiane_ritos_coverage")
    public LaianeRitosCoverageResponse coverage() {
        Set<String> defined = ritoPackService.definitions().keySet();
        List<String> supported = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String ritoName : ritoPackService.catalogDrivenRitos().stream().map(Enum::name).toList()) {
            if (defined.contains(ritoName)) supported.add(ritoName);
            else missing.add(ritoName);
        }

        Collections.sort(supported);
        Collections.sort(missing);

        return LaianeRitosCoverageResponse.builder()
                .supported(supported)
                .missingPackDefinition(missing)
                .build();
    }
}
