package com.tcc.pjb.backend.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IntimacaoMulticanalServiceTest {

    @Test
    void dispatchRetornaDocumentoFormalAssinado() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        OfficialDocumentTemplateService officialDocumentTemplateService = Mockito.mock(OfficialDocumentTemplateService.class);

        Processo processo = new Processo();
        processo.setId(15L);
        processo.setNumeroProcesso("0000150-22.2026.8.06.0001");
        Usuario usuario = new Usuario();
        usuario.setId(31L);
        usuario.setNome("Parte Intimada");

        when(processoRepository.findById(15L)).thenReturn(Optional.of(processo));
        when(usuarioRepository.findById(31L)).thenReturn(Optional.of(usuario));
        when(notificationService.notifyUserAdvanced(any(), any(), any(), any(), any(), any(Boolean.class)))
                .thenReturn(new NotificationService.DispatchReport(
                        List.of("EMAIL"),
                        List.of(),
                        List.of(),
                        List.of("TK-1"),
                        "ENVIADA",
                        List.of("ok")
                ));
        when(officialDocumentTemplateService.renderizar(any())).thenReturn(new OfficialDocumentTemplateRenderResponse(
                15L,
                processo.getNumeroProcesso(),
                TemplateDocumentoOficial.INTIMACAO_FORMAL,
                "Intimação formal multicanal",
                List.of(),
                List.of(),
                "CONTEUDO_ASSINADO",
                "ab".repeat(32),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                DocumentoCategoria.PUBLICO,
                NivelSigilo.PUBLICO,
                true,
                true,
                List.of(),
                Map.of("rubrica", "PJB-RUB-TESTE", "envelopeId", "PJB-ENV-TESTE"),
                Map.of("status", "VALIDO")
        ));

        IntimacaoMulticanalService service = new IntimacaoMulticanalService(
                processoRepository,
                usuarioRepository,
                notificationService,
                authorizationService,
                officialDocumentTemplateService
        );

        var response = service.dispatch(15L, 31L, "Título", "Mensagem", "https://pjb.local/acesso", true);

        assertThat(response.documentoFormalAssinado()).containsEntry("template", "INTIMACAO_FORMAL");
        assertThat(response.assinaturaQualificada()).containsEntry("rubrica", "PJB-RUB-TESTE");
        assertThat(response.validacaoSoberana()).containsEntry("status", "VALIDO");
    }
}
