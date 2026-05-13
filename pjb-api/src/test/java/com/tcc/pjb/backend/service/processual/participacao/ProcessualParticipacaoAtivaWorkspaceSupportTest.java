package com.tcc.pjb.backend.service.processual.participacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.processual.participacao.workspace.ProcessualParticipacaoAtivaWorkspaceSupport;
import com.tcc.pjb.backend.service.processual.participacao.workspace.RepresentationGuardView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.SecurityGuardView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessualParticipacaoAtivaWorkspaceSupportTest {

    @Mock
    private DocumentoProcessualRepository documentoRepository;
    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private RepresentacaoProcessualPolicyService representacaoPolicyService;

    private ProcessualParticipacaoAtivaWorkspaceSupport support;

    @BeforeEach
    void setUp() {
        support = new ProcessualParticipacaoAtivaWorkspaceSupport(documentoRepository, workItemRepository, representacaoPolicyService);
    }

    @Test
    void segurancaSigilosaDeveExigirCertificadoEStepUp() {
        Processo processo = processoBase(70L, NivelSigilo.SIGILO_N4);

        SignaturePolicy signaturePolicy = support.buildSignaturePolicy(Persona.PROCURADORIA, processo);
        SecurityGuardView security = support.buildSecurityGuard(processo, signaturePolicy, null, null);

        assertEquals("ALTA_RESTRICAO", security.classificacao());
        assertTrue(security.certificadoObrigatorio());
        assertTrue(security.stepUpObrigatorio());
        assertTrue(security.restritoAAtuacaoInstitucional());
    }

    @Test
    void representacaoPrivadaSemMandatoDeveExigirConferencia() {
        Processo processo = processoBase(71L, NivelSigilo.PUBLICO);
        Usuario usuario = usuarioBase(7L, TipoUsuario.ADVOGADO);

        when(representacaoPolicyService.resolve(eq(processo), eq(usuario), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new RepresentacaoProcessualPolicyResponse(
                        "MANDATO_AD_JUDICIA",
                        "PROCURACAO_PUBLICA",
                        "POSTULACAO_TECNICA",
                        "ADVOGADO",
                        "CIVEL",
                        "COMUM",
                        "TJCE",
                        true,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        List.of("CPC"),
                        List.of("procuração"),
                        List.of(),
                        List.of("validar_vinculo"),
                        List.of("Procuração pendente"),
                        List.of("APRESENTAR_MANIFESTACAO"),
                        java.util.Map.of()
                ));

        RepresentationGuardView view = support.buildRepresentationGuard(processo, usuario, Persona.ADVOCACIA_PRIVADA, null, false);

        assertEquals("CONFERENCIA_REPRESENTACAO_REQUERIDA", view.status());
        assertTrue(view.validacoesObrigatorias().contains("COMPROVAR_VINCULO_REPRESENTATIVO_ATIVO"));
    }

    private static Processo processoBase(Long id, NivelSigilo sigilo) {
        Processo processo = new Processo();
        processo.setId(id);
        processo.setTribunal("TJCE");
        processo.setComarca("Fortaleza");
        processo.setVara("1ª Vara Cível");
        processo.setUf("CE");
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
