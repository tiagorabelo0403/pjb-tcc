package com.tcc.pjb.backend.service.secretariat.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SecretariatOperationalExpeditionBatchServiceTest {

    @Test
    void materializaLoteComDocumentosFormaisAssinados() {
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        SecretariatQueueProjectionService projectionService = Mockito.mock(SecretariatQueueProjectionService.class);
        OfficialDocumentTemplateService officialDocumentTemplateService = Mockito.mock(OfficialDocumentTemplateService.class);
        when(workItemRepository.findLatestByProcessoIdAndTemplateCode(anyLong(), anyString())).thenReturn(Optional.empty());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(900L + ((long) item.getTemplateCode().hashCode() & 1023L));
            }
            return item;
        });
        when(officialDocumentTemplateService.renderizar(any())).thenReturn(new OfficialDocumentTemplateRenderResponse(
                501L,
                "0001111-22.2026.8.06.0001",
                TemplateDocumentoOficial.CERTIDAO,
                "Documento formal",
                List.of(),
                List.of(),
                "CONTEUDO_ASSINADO",
                "ab".repeat(32),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                DocumentoCategoria.PUBLICO,
                NivelSigilo.PUBLICO,
                true,
                true,
                List.of(),
                Map.of("rubrica", "PJB-RUB-TESTE", "envelopeId", "PJB-ENV-TESTE"),
                Map.of("status", "VALIDO", "documentoAssinadoHash", "cd".repeat(32))
        ));
        SecretariatOperationalExpeditionBatchService service = new SecretariatOperationalExpeditionBatchService(
                workItemRepository,
                projectionService,
                officialDocumentTemplateService
        );

        var execution = service.materializar(
                processo(),
                actor(),
                routing(),
                new SecretariatOperationalChecklistEngine.ChecklistSnapshot(List.of(), List.of("PENDENCIA_DOCUMENTAL"), List.of(), Map.of()),
                new SecretariatOperationalActLineService.ActLineSnapshot(List.of(), List.of(), Map.of()),
                "PADRAO"
        );

        assertThat(execution.generatedWorkItemIds()).isNotEmpty();
        assertThat(execution.documentosFormaisAssinados()).isNotEmpty();
        assertThat(execution.documentosFormaisAssinados())
                .extracting(item -> item.get("templateDocumentoOficial"))
                .contains("CERTIDAO", "INTIMACAO_FORMAL", "OFICIO");
    }

    private static Processo processo() {
        Processo processo = new Processo();
        processo.setId(501L);
        processo.setNumeroProcesso("0001111-22.2026.8.06.0001");
        processo.setNumeroUnificado("0001111-22.2026.8.06.0001");
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setRamoDireito(RamoDireito.ADMINISTRATIVO);
        processo.setNivelSigilo(NivelSigilo.SEGREDO_JUSTICA);
        processo.setUf("CE");
        processo.setComarca("Quixadá");
        return processo;
    }

    private static Usuario actor() {
        Usuario usuario = new Usuario();
        usuario.setId(77L);
        usuario.setNome("Servidor de Secretaria");
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        usuario.setUf("CE");
        usuario.setComarca("Quixadá");
        return usuario;
    }

    private static SecretariatOperationalRoutingProfile routing() {
        return new SecretariatOperationalRoutingProfile(
                "ROTA-SECRETARIA",
                "ESTADUAL",
                "TJCE",
                "1G",
                "COMUM",
                "FAZENDA",
                "EXECUCAO",
                "SECRETARIA_FAZENDA",
                "RCV",
                "INBOX_RCV",
                "SAN",
                "INBOX_SAN",
                "AUD",
                "INBOX_AUD",
                "EXEC",
                "INBOX_EXEC",
                "AUD-01",
                "/TJCE/QUX/SECRETARIA",
                Duration.ofHours(4),
                Duration.ofHours(8),
                Duration.ofHours(6),
                30,
                true,
                true,
                true,
                true,
                List.of(),
                List.of(),
                new SecretariatSpecializationResolver().resolve(
                        "TJCE",
                        "PRIMEIRO_GRAU",
                        "JUSTICA_ESTADUAL",
                        "FAZENDA",
                        "SECRETARIA_FAZENDA",
                        "INBOX_RCV",
                        "SAN",
                        "AUD",
                        "EXEC",
                        "/TJCE/QUX/SECRETARIA",
                        Map.of("laneAxis", "FAZENDA", "forumAxis", "FORO_COMUM", "unitDescriptor", "1a Vara da Fazenda")
                ),
                JudicialScaleProfile.VARA_1G,
                Map.of()
        );
    }
}
