package com.tcc.pjb.backend.service.processual.comunicacao.institutional.panel;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalExecutivePanelApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalExecutiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelNotificationResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalPanelFacadeService {

    private final InstitutionalExecutivePanelApplicationService service;
    private final NationalCommunicationInstitutionalPanelAssemblerSupport assemblerSupport;

    public NationalCommunicationInstitutionalPanelFacadeService(InstitutionalExecutivePanelApplicationService service,
                                                                NationalCommunicationInstitutionalPanelAssemblerSupport assemblerSupport) {
        this.service = Objects.requireNonNull(service);
        this.assemblerSupport = Objects.requireNonNull(assemblerSupport);
    }

    public NationalCommunicationInstitutionalExecutiveDashboardResponse painelExecutivo(String unidadeCodigo,
                                                                                         String uf,
                                                                                         DestinatarioInstitucionalKind destinatarioKind,
                                                                                         Long processoId,
                                                                                         String expedicaoUuid) {
        return assemblerSupport.toResponse(service.painelExecutivo(unidadeCodigo, uf, destinatarioKind, processoId, expedicaoUuid));
    }

    public List<NationalCommunicationInstitutionalPanelNotificationResponse> notificacoes(String unidadeCodigo,
                                                                                           String uf,
                                                                                           DestinatarioInstitucionalKind destinatarioKind,
                                                                                           Long processoId,
                                                                                           String expedicaoUuid) {
        return service.notificacoes(unidadeCodigo, uf, destinatarioKind, processoId, expedicaoUuid).stream().map(assemblerSupport::toResponse).toList();
    }
}
