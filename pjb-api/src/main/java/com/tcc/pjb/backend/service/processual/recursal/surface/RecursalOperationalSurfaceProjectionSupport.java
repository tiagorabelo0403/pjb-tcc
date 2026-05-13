package com.tcc.pjb.backend.service.processual.recursal.surface;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalOperationalSurfaceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalOperationalSurfaceSectionView;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecursalOperationalSurfaceProjectionSupport {

    private final RecursalAutomationWorkspaceService workspaceService;

    public RecursalOperationalSurfaceProjectionSupport(RecursalAutomationWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public RecursalOperationalSurfaceResponse buildAggregate(RecursalAutomationRequest request) {
        RecursalAutomationWorkspaceResponse workspace = workspaceService.buildWorkspace(request);
        List<RecursalOperationalSurfaceSectionView> secoes = RecursalOperationalSurfaceCatalog.all().stream()
                .map(axis -> buildSection(axis, workspace))
                .toList();
        boolean operationallyBlocked = workspace.poderRecorrerBloqueado() || requiresOperationalPreparoLock(request);
        String motivoBloqueio = workspace.poderRecorrerBloqueado()
                ? workspace.motivoBloqueioPoderRecorrer()
                : operationallyBlocked
                ? "preparo recursal não confirmado impede liberação operacional da surface de protocolo"
                : workspace.motivoBloqueioPoderRecorrer();
        return new RecursalOperationalSurfaceResponse(
                workspace.rotaPrioritaria(),
                workspace.nomenclaturaAtiva(),
                operationallyBlocked,
                motivoBloqueio,
                List.copyOf(secoes),
                RecursalOperationalSurfaceCatalog.aggregatedGaps(operationallyBlocked)
        );
    }


    private boolean requiresOperationalPreparoLock(RecursalAutomationRequest request) {
        return request != null
                && !request.preparoEfetuado()
                && !request.processoFisico()
                && !request.recursoPrincipalInterposto()
                && !request.recursoPrincipalConhecido();
    }

    public RecursalSpecializedSurfaceResponse buildSpecialized(
            RecursalAutomationRequest request,
            RecursalOperationalSurfaceAxisDefinition axis) {
        RecursalAutomationWorkspaceResponse workspace = workspaceService.buildWorkspace(request);
        List<RecursalAutomationWorkspaceTrackView> selecionadas = selectedTracks(axis, workspace);
        LinkedHashSet<String> secoesObrigatorias = selecionadas.stream()
                .flatMap(track -> track.secoesObrigatorias().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> alertas = selecionadas.stream()
                .flatMap(track -> track.alertasTaticos().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new RecursalSpecializedSurfaceResponse(
                axis.codigo(),
                axis.titulo(),
                axis.rota(),
                workspace.rotaPrioritaria(),
                workspace.nomenclaturaAtiva(),
                workspace.poderRecorrerBloqueado(),
                workspace.motivoBloqueioPoderRecorrer(),
                selecionadas.stream().map(RecursalAutomationWorkspaceTrackView::codigo).toList(),
                List.copyOf(secoesObrigatorias),
                List.copyOf(alertas),
                axis.gaps()
        );
    }

    private RecursalOperationalSurfaceSectionView buildSection(
            RecursalOperationalSurfaceAxisDefinition axis,
            RecursalAutomationWorkspaceResponse workspace) {
        List<RecursalAutomationWorkspaceTrackView> selecionadas = selectedTracks(axis, workspace);
        LinkedHashSet<String> secoesObrigatorias = selecionadas.stream()
                .flatMap(track -> track.secoesObrigatorias().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> alertas = selecionadas.stream()
                .flatMap(track -> track.alertasTaticos().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new RecursalOperationalSurfaceSectionView(
                axis.codigo(),
                axis.titulo(),
                selecionadas.stream().map(RecursalAutomationWorkspaceTrackView::codigo).toList(),
                List.copyOf(secoesObrigatorias),
                List.copyOf(alertas)
        );
    }

    private List<RecursalAutomationWorkspaceTrackView> selectedTracks(
            RecursalOperationalSurfaceAxisDefinition axis,
            RecursalAutomationWorkspaceResponse workspace) {
        return workspace.trilhas().stream()
                .filter(track -> axis.trilhas().contains(track.codigo()))
                .toList();
    }
}
