package com.tcc.pjb.backend.core.comunicacao.institucional.processual.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspaceSummary;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalProcessWorkspaceApplicationService {

    private final InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService;
    private final InstitutionalProcessWorkspaceSnapshotResolver snapshotResolver;
    private final InstitutionalProcessWorkspaceAssembler workspaceAssembler;
    private final InstitutionalProcessWorkspaceDiagnosticResolver diagnosticResolver;

    public InstitutionalProcessWorkspaceApplicationService(InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService,
                                                           InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService,
                                                           ProcessoRepository processoRepository) {
        this.accessProfileCatalogApplicationService = Objects.requireNonNull(accessProfileCatalogApplicationService);
        this.snapshotResolver = new InstitutionalProcessWorkspaceSnapshotResolver(Objects.requireNonNull(processoRepository));
        this.workspaceAssembler = new InstitutionalProcessWorkspaceAssembler(Objects.requireNonNull(panelBlueprintApplicationService));
        this.diagnosticResolver = new InstitutionalProcessWorkspaceDiagnosticResolver();
    }

    public List<InstitutionalProcessWorkspaceSummary> listarPerfis(Long processoId,
                                                                   String rito,
                                                                   String fase,
                                                                   String status,
                                                                   String ramo) {
        InstitutionalProcessWorkspaceSnapshot snapshot = snapshotResolver.loadSnapshot(processoId, rito, fase, status, ramo);
        return accessProfileCatalogApplicationService.listarPerfis().stream()
                .map(entry -> workspaceAssembler.summarize(entry, snapshot))
                .sorted(Comparator.comparing(InstitutionalProcessWorkspaceSummary::displayName))
                .toList();
    }

    public InstitutionalProcessWorkspace detalharPerfil(String profileCode,
                                                        Long processoId,
                                                        String rito,
                                                        String fase,
                                                        String status,
                                                        String ramo) {
        InstitutionalAccessProfileCatalogEntry entry = accessProfileCatalogApplicationService.listarPerfis().stream()
                .filter(item -> item.codigo().equalsIgnoreCase(profileCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Perfil processual institucional não encontrado: " + profileCode));
        return workspaceAssembler.toWorkspace(entry, snapshotResolver.loadSnapshot(processoId, rito, fase, status, ramo));
    }

    public InstitutionalProcessDiagnosticReport diagnosticar(Long processoId,
                                                             String rito,
                                                             String fase,
                                                             String status,
                                                             String ramo) {
        InstitutionalProcessWorkspaceSnapshot snapshot = snapshotResolver.loadSnapshot(processoId, rito, fase, status, ramo);
        return diagnosticResolver.diagnosticar(accessProfileCatalogApplicationService.listarPerfis(), workspaceAssembler, snapshot);
    }
}
