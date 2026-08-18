package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalScopeResolutionSupport;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.catalog.ApoioInstitucionalPanelBlueprintCatalog;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.catalog.DefensoriaEProcuradoriaInstitutionalPanelBlueprintCatalog;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.catalog.ForumInstitutionalPanelBlueprintCatalog;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.catalog.InstitutionalPanelBlueprintCatalog;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.catalog.MinisterioPublicoInstitutionalPanelBlueprintCatalog;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

@Service
public class InstitutionalPanelBlueprintApplicationService {

    private final List<InstitutionalPanelBlueprintCatalog> catalogs;

    @Inject
    public InstitutionalPanelBlueprintApplicationService() {
        this(defaultCatalogs());
    }

    public InstitutionalPanelBlueprintApplicationService(List<InstitutionalPanelBlueprintCatalog> catalogs) {
        this.catalogs = List.copyOf(catalogs == null || catalogs.isEmpty() ? defaultCatalogs() : catalogs);
    }

    public List<InstitutionalPanelBlueprintSpec> listar(String scope, String panel) {
        return catalogs.stream()
                .flatMap(catalog -> catalog.specs().stream())
                .filter(item -> InstitutionalScopeResolutionSupport.matchesFilter(item.escopo(), scope))
                .filter(item -> panel == null || panel.isBlank() || item.panel().equalsIgnoreCase(panel))
                .sorted(Comparator.comparing(InstitutionalPanelBlueprintSpec::codigo))
                .toList();
    }

    private static List<InstitutionalPanelBlueprintCatalog> defaultCatalogs() {
        return List.of(
                new ForumInstitutionalPanelBlueprintCatalog(),
                new MinisterioPublicoInstitutionalPanelBlueprintCatalog(),
                new DefensoriaEProcuradoriaInstitutionalPanelBlueprintCatalog(),
                new ApoioInstitucionalPanelBlueprintCatalog()
        );
    }
}
