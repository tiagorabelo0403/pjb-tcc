package com.tcc.pjb.backend.core.comunicacao.institucional.hardening;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.MatrizCapacidadeCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryDeadLetterStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure.InstitutionalDeliveryJobStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application.InstitutionalCommunicationHardeningApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure.InstitutionalExternalAdapter;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure.InstitutionalExternalDispatchStateRepository;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class InstitutionalCommunicationHardeningApplicationServiceTest {

    @Test
    void shouldApproveWhenCoverageAndBacklogAreHealthy() {
        CatalogoInstitucionalUnificadoService catalogo = new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService.class));
        InstitutionalInboxStateRepository inboxRepository = Mockito.mock(InstitutionalInboxStateRepository.class);
        InstitutionalGateStateRepository gateRepository = Mockito.mock(InstitutionalGateStateRepository.class);
        InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository = Mockito.mock(InstitutionalDeliveryDeadLetterStateRepository.class);
        InstitutionalDeliveryJobStateRepository jobRepository = Mockito.mock(InstitutionalDeliveryJobStateRepository.class);
        InstitutionalExternalDispatchStateRepository externalRepository = Mockito.mock(InstitutionalExternalDispatchStateRepository.class);
        when(inboxRepository.findAll()).thenReturn(List.of());
        when(gateRepository.findAll()).thenReturn(List.of());
        when(deadLetterRepository.findAll()).thenReturn(List.of());
        when(jobRepository.findAll()).thenReturn(List.of());
        when(externalRepository.findAll()).thenReturn(List.of());
        InstitutionalCommunicationHardeningApplicationService service = new InstitutionalCommunicationHardeningApplicationService(
                catalogo,
                inboxRepository,
                gateRepository,
                deadLetterRepository,
                jobRepository,
                externalRepository,
                new MatrizCapacidadeCaixaInstitucionalService(),
                List.of(
                        stubAdapter(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO),
                        stubAdapter(CanalComunicacaoInstitucional.DJEN),
                        stubAdapter(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL),
                        stubAdapter(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL)
                )
        );

        var report = service.gerarRelatorio();

        assertTrue(report.aprovado());
        assertTrue(report.findings().isEmpty());
    }

    @Test
    void shouldRaiseBlockingFindingWhenExternalCoverageIsMissing() {
        CatalogoInstitucionalUnificadoService catalogo = new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService.class));
        InstitutionalInboxStateRepository inboxRepository = Mockito.mock(InstitutionalInboxStateRepository.class);
        InstitutionalGateStateRepository gateRepository = Mockito.mock(InstitutionalGateStateRepository.class);
        InstitutionalDeliveryDeadLetterStateRepository deadLetterRepository = Mockito.mock(InstitutionalDeliveryDeadLetterStateRepository.class);
        InstitutionalDeliveryJobStateRepository jobRepository = Mockito.mock(InstitutionalDeliveryJobStateRepository.class);
        InstitutionalExternalDispatchStateRepository externalRepository = Mockito.mock(InstitutionalExternalDispatchStateRepository.class);
        when(inboxRepository.findAll()).thenReturn(List.of());
        when(gateRepository.findAll()).thenReturn(List.of());
        when(deadLetterRepository.findAll()).thenReturn(List.of());
        when(jobRepository.findAll()).thenReturn(List.of());
        when(externalRepository.findAll()).thenReturn(List.of());
        InstitutionalCommunicationHardeningApplicationService service = new InstitutionalCommunicationHardeningApplicationService(
                catalogo,
                inboxRepository,
                gateRepository,
                deadLetterRepository,
                jobRepository,
                externalRepository,
                new MatrizCapacidadeCaixaInstitucionalService(),
                List.of(stubAdapter(CanalComunicacaoInstitucional.DJEN))
        );

        var report = service.gerarRelatorio();

        assertFalse(report.aprovado());
        assertTrue(report.findings().stream().anyMatch(finding -> finding.code().equals("EXTERNAL_CHANNEL_COVERAGE")));
    }

    private InstitutionalExternalAdapter stubAdapter(CanalComunicacaoInstitucional channel) {
        return new InstitutionalExternalAdapter() {
            @Override
            public boolean supports(CanalComunicacaoInstitucional candidate) {
                return candidate == channel;
            }

            @Override
            public InstitutionalExternalDispatchResult dispatch(com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch dispatch) {
                return InstitutionalExternalDispatchResult.accepted("stub", "OK", "{}");
            }
        };
    }
}
