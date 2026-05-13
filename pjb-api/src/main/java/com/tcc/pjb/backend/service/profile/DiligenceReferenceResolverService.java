package com.tcc.pjb.backend.service.profile;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalLinkResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;

@Service
public class DiligenceReferenceResolverService {

    private final WorkItemRepository workItemRepository;

    public DiligenceReferenceResolverService(WorkItemRepository workItemRepository) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
    }

    public Optional<ResolvedDiligenceReference> resolve(TelemetriaOperacionalCanal canal,
                                                        String diligenceReference) {
        if (canal == null || diligenceReference == null || diligenceReference.isBlank()) {
            return Optional.empty();
        }
        Long workItemId = parseId(diligenceReference);
        if (workItemId == null) {
            return Optional.empty();
        }
        return workItemRepository.findById(workItemId)
                .filter(item -> matchesChannel(canal, item))
                .map(item -> new ResolvedDiligenceReference(
                        item.getId(),
                        processId(item.getProcesso()),
                        processNumber(item.getProcesso()),
                        item.getTemplateCode(),
                        item.getType() != null ? item.getType().name() : null,
                        item.getStatus() != null ? item.getStatus().name() : null,
                        item
                ));
    }

    public DiligenceOperationalLinkResponse describe(TelemetriaOperacionalCanal canal,
                                                     String diligenceReference) {
        return resolve(canal, diligenceReference)
                .map(link -> {
                    WorkItem item = link.workItem();
                    return new DiligenceOperationalLinkResponse(
                            true,
                            canal.name(),
                            diligenceReference.trim(),
                            link.workItemId(),
                            link.processoId(),
                            link.processoNumero(),
                            link.templateCode(),
                            link.workItemType(),
                            link.workItemStatus(),
                            item != null && item.getAssignedRole() != null ? item.getAssignedRole().name() : null,
                            item != null && item.getAssignedUser() != null ? item.getAssignedUser().getNome() : null,
                            item != null ? item.getDueAt() : null,
                            item != null ? item.getUf() : null,
                            item != null ? item.getComarca() : null
                    );
                })
                .orElseGet(() -> new DiligenceOperationalLinkResponse(
                        false,
                        canal != null ? canal.name() : null,
                        diligenceReference == null ? null : diligenceReference.trim(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
    }

    private boolean matchesChannel(TelemetriaOperacionalCanal canal,
                                   WorkItem item) {
        if (item == null) {
            return false;
        }
        String title = item.getTitulo() == null ? "" : item.getTitulo().toUpperCase(Locale.ROOT);
        WorkItemType type = item.getType();
        return switch (canal) {
            case OFICIAL_JUSTICA -> title.contains("MANDADO")
                    || title.contains("CITACAO")
                    || title.contains("INTIMACAO")
                    || title.contains("BUSCA")
                    || title.contains("PENHORA")
                    || type == WorkItemType.CITACAO
                    || type == WorkItemType.INTIMACAO
                    || type == WorkItemType.EXPEDICAO
                    || type == WorkItemType.DILIGENCIA;
            case DELEGADO -> title.contains("DILIGENCIA")
                    || title.contains("MANDADO")
                    || title.contains("BUSCA")
                    || title.contains("INQUERITO")
                    || type == WorkItemType.DILIGENCIA
                    || type == WorkItemType.EXPEDICAO
                    || type == WorkItemType.CITACAO
                    || type == WorkItemType.INTIMACAO;
        };
    }

    private Long parseId(String diligenceReference) {
        try {
            return Long.parseLong(diligenceReference.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long processId(Processo processo) {
        return processo != null ? processo.getId() : null;
    }

    private String processNumber(Processo processo) {
        return processo != null ? processo.getNumeroProcesso() : null;
    }

    public record ResolvedDiligenceReference(
            Long workItemId,
            Long processoId,
            String processoNumero,
            String templateCode,
            String workItemType,
            String workItemStatus,
            WorkItem workItem
    ) {
    }
}
