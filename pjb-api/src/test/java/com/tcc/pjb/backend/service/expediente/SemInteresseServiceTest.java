package com.tcc.pjb.backend.service.expediente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SemInteresseServiceTest {

    @Test
    void retornaEnvelopeQualificadoNaManifestacao() {
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        OfficialDocumentTemplateService officialDocumentTemplateService = Mockito.mock(OfficialDocumentTemplateService.class);
        NotificationService notificationService = Mockito.mock(NotificationService.class);

        Processo processo = new Processo();
        processo.setId(70L);
        processo.setNumeroProcesso("0000700-22.2026.8.06.0001");
        processo.setUsuario(new Usuario());

        WorkItem expediente = WorkItem.builder().id(99L).processo(processo).status(WorkItemStatus.PENDENTE).build();
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Advogado Autor");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);

        when(workItemRepository.findById(99L)).thenReturn(Optional.of(expediente));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(officialDocumentTemplateService.renderizar(any())).thenReturn(new OfficialDocumentTemplateRenderResponse(
                70L,
                processo.getNumeroProcesso(),
                TemplateDocumentoOficial.SEM_INTERESSE_MANIFESTACAO,
                "Manifestação de sem interesse",
                List.of(),
                List.of(),
                "CONTEUDO_ASSINADO",
                "ab".repeat(32),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                DocumentoCategoria.PUBLICO,
                NivelSigilo.PUBLICO,
                true,
                true,
                List.of(),
                Map.of("rubrica", "PJB-RUB-TESTE", "envelopeId", "PJB-ENV-TESTE"),
                Map.of("status", "VALIDO")
        ));

        SemInteresseService service = new SemInteresseService(workItemRepository, currentUserService, officialDocumentTemplateService, notificationService);
        var response = service.registrar(99L, "Sem interesse na medida");

        assertThat(response.documentoId()).isNotNull();
        assertThat(response.assinaturaQualificada()).containsEntry("rubrica", "PJB-RUB-TESTE");
        assertThat(response.validacaoSoberana()).containsEntry("status", "VALIDO");
        assertThat(response.semInteresse()).isTrue();
    }
}
