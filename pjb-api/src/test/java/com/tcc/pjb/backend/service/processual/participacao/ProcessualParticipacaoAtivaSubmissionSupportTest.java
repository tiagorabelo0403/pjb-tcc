package com.tcc.pjb.backend.service.processual.participacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.processual.participacao.submission.AttachmentRequest;
import com.tcc.pjb.backend.service.processual.participacao.submission.PreparedPrimaryDocument;
import com.tcc.pjb.backend.service.processual.participacao.submission.ProcessualParticipacaoAtivaSubmissionSupport;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionRequest;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessualParticipacaoAtivaSubmissionSupportTest {

    @Mock
    private DocumentoProcessualRepository documentoRepository;
    @Mock
    private EntityManager entityManager;

    private ProcessualParticipacaoAtivaSubmissionSupport support;

    @BeforeEach
    void setUp() {
        support = new ProcessualParticipacaoAtivaSubmissionSupport(documentoRepository, entityManager);
    }

    @Test
    void deveBloquearAnexosDuplicadosNoMesmoLote() {
        Processo processo = processoBase(90L, NivelSigilo.PUBLICO);
        Usuario usuario = usuarioBase(12L, TipoUsuario.ADVOGADO);
        when(entityManager.getReference(Processo.class, 90L)).thenReturn(processo);
        when(documentoRepository.existsByProcessoIdAndSha256(eq(90L), any())).thenReturn(false);

        byte[] pdf = "MESMO-CONTEUDO".getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(pdf);
        List<AttachmentRequest> anexos = List.of(
                new AttachmentRequest("a.pdf", "application/pdf", base64, "A", null, null),
                new AttachmentRequest("b.pdf", "application/pdf", base64, "B", null, null)
        );

        ActionProfile action = new ActionProfile("APRESENTAR_MANIFESTACAO", "Apresentar manifestação", null, 1, false, false, List.of(), List.of(), List.of(), List.of(), null);

        assertThrows(ErroDeValidacaoException.class, () -> support.prepareAttachments(processo, usuario, Persona.ADVOCACIA_PRIVADA, action, anexos));
    }

    @Test
    void documentoPrincipalSensivelDeveSerMarcadoComoPessoal() {
        Processo processo = processoBase(91L, NivelSigilo.SIGILO_N2);
        Usuario usuario = usuarioBase(13L, TipoUsuario.ADVOGADO);
        when(entityManager.getReference(Processo.class, 91L)).thenReturn(processo);
        when(documentoRepository.existsByProcessoIdAndSha256(eq(91L), any())).thenReturn(false);

        SubmissionRequest request = new SubmissionRequest(
                "APRESENTAR_MANIFESTACAO",
                "Petição sensível",
                "Conteúdo protegido",
                null,
                "SIGILO_N2",
                false,
                "ICP_BRASIL",
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
                null,
                null,
                null,
                false,
                null,
                null,
                List.of()
        );

        ActionProfile action = new ActionProfile("APRESENTAR_MANIFESTACAO", "Apresentar manifestação", null, 1, false, true, List.of(), List.of(), List.of(), List.of(), null);
        PreparedPrimaryDocument prepared = support.preparePrimaryDocument(processo, usuario, Persona.ADVOCACIA_PRIVADA, action, request);

        assertEquals("PESSOAL", prepared.documento().getCategoria().name());
        assertEquals("SIGILO_N2", prepared.documento().getNivelSigilo().name());
    }

    private static Processo processoBase(Long id, NivelSigilo sigilo) {
        Processo processo = new Processo();
        processo.setId(id);
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setNumeroUnificado("0001234-56.2026.8.06.0001");
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setNivelSigilo(sigilo);
        return processo;
    }

    private static Usuario usuarioBase(Long id, TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setCpf(UUID.randomUUID().toString());
        return usuario;
    }
}
