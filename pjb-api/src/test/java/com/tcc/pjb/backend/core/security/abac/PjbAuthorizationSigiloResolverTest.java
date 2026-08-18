package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PjbAuthorizationSigiloResolverTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private SigiloAccessService sigiloAccessService;

    private PjbAuthorizationSigiloResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PjbAuthorizationSigiloResolver(currentUserService, sigiloAccessService);
    }

    @Test
    void computeProcessoSigiloEfetivo_nullRetornaPublico() {
        assertThat(resolver.computeProcessoSigiloEfetivo(null)).isEqualTo(NivelSigilo.PUBLICO);
    }

    @Test
    void computeProcessoSigiloEfetivo_nivelSigiloNullRetornaPublico() {
        Processo p = Processo.builder().nivelSigilo(null).build();
        assertThat(resolver.computeProcessoSigiloEfetivo(p)).isEqualTo(NivelSigilo.PUBLICO);
    }

    @Test
    void computeProcessoSigiloEfetivo_sigiloN2RetornaN2() {
        Processo p = Processo.builder().nivelSigilo(NivelSigilo.SIGILO_N2).build();
        assertThat(resolver.computeProcessoSigiloEfetivo(p)).isEqualTo(NivelSigilo.SIGILO_N2);
    }

    @Test
    void computeProcessoSigiloEfetivo_segredoJusticaRetornaSegredo() {
        Processo p = Processo.builder().nivelSigilo(NivelSigilo.SEGREDO_JUSTICA).build();
        assertThat(resolver.computeProcessoSigiloEfetivo(p)).isEqualTo(NivelSigilo.SEGREDO_JUSTICA);
    }

    @Test
    void elevateProcessoSigilo_publicoElevadoParaN2() {
        Processo p = Processo.builder().nivelSigilo(NivelSigilo.PUBLICO).build();
        assertThat(resolver.elevateProcessoSigilo(p, NivelSigilo.SIGILO_N2)).isEqualTo(NivelSigilo.SIGILO_N2);
    }

    @Test
    void elevateProcessoSigilo_N3MaxVenceN2() {
        Processo p = Processo.builder().nivelSigilo(NivelSigilo.SIGILO_N3).build();
        assertThat(resolver.elevateProcessoSigilo(p, NivelSigilo.SIGILO_N2)).isEqualTo(NivelSigilo.SIGILO_N3);
    }

    @Test
    void shadowProcesso_nullRetornaNull() {
        assertThat(resolver.shadowProcesso(null, NivelSigilo.SIGILO_N2)).isNull();
    }

    @Test
    void shadowProcesso_copiaComMesmoIdENovoNivel() {
        Processo base = Processo.builder()
                .id(42L)
                .numeroProcesso("0001234-55.2024.8.06.0001")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
        Processo shadow = resolver.shadowProcesso(base, NivelSigilo.SIGILO_N2);
        assertThat(shadow).isNotSameAs(base);
        assertThat(shadow.getId()).isEqualTo(42L);
        assertThat(shadow.getNumeroProcesso()).isEqualTo("0001234-55.2024.8.06.0001");
        assertThat(shadow.getNivelSigilo()).isEqualTo(NivelSigilo.SIGILO_N2);
    }

    @Test
    void computeDocumentoSigiloEfetivo_documentoNullRetornaPublico() {
        assertThat(resolver.computeDocumentoSigiloEfetivo(null, null)).isEqualTo(NivelSigilo.PUBLICO);
    }

    @Test
    void computeDocumentoSigiloEfetivo_categorPessoalProcessoPublicoRetornaMinN2() {
        Processo p = Processo.builder().nivelSigilo(NivelSigilo.PUBLICO).build();
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setCategoria(DocumentoCategoria.PESSOAL);
        doc.setNivelSigilo(NivelSigilo.PUBLICO);
        NivelSigilo result = resolver.computeDocumentoSigiloEfetivo(p, doc);
        assertThat(result.getNivel()).isGreaterThanOrEqualTo(NivelSigilo.SIGILO_N2.getNivel());
    }

    @Test
    void computeDocumentoSigiloEfetivo_docN3RetornaMinN3() {
        Processo p = Processo.builder().nivelSigilo(NivelSigilo.PUBLICO).build();
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setCategoria(DocumentoCategoria.PUBLICO);
        doc.setNivelSigilo(NivelSigilo.SIGILO_N3);
        NivelSigilo result = resolver.computeDocumentoSigiloEfetivo(p, doc);
        assertThat(result.getNivel()).isGreaterThanOrEqualTo(NivelSigilo.SIGILO_N3.getNivel());
    }

    @Test
    void assessStepUp_sigiloPublico_notRequired() {
        PjbAuthorizationStepUpAssessment assessment =
                resolver.assessStepUpForSigiloEfetivo(null, NivelSigilo.PUBLICO);
        assertThat(assessment.required()).isFalse();
        assertThat(assessment.satisfied()).isTrue();
        assertThat(assessment.deniedByStepUp()).isFalse();
    }

    @Test
    void assessStepUp_sigiloN2_magistrado_notRequired() {
        Usuario magistrado = new Usuario();
        magistrado.setId(1L);
        magistrado.setTipoUsuario(TipoUsuario.MAGISTRADO);
        when(currentUserService.getOrNull()).thenReturn(magistrado);

        PjbAuthorizationStepUpAssessment assessment =
                resolver.assessStepUpForSigiloEfetivo(null, NivelSigilo.SIGILO_N2);

        assertThat(assessment.required()).isFalse();
        assertThat(assessment.satisfied()).isTrue();
        assertThat(assessment.code()).isEqualTo("institucional_privilegiado");
    }

    @Test
    void assessStepUp_sigiloN2_advogado_semCredencial_requiredButMissing() {
        Usuario advogado = new Usuario();
        advogado.setId(99L);
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);
        when(currentUserService.getOrNull()).thenReturn(advogado);

        PjbAuthorizationStepUpAssessment assessment =
                resolver.assessStepUpForSigiloEfetivo(Processo.builder().build(), NivelSigilo.SIGILO_N2);

        assertThat(assessment.required()).isTrue();
        assertThat(assessment.satisfied()).isFalse();
        assertThat(assessment.deniedByStepUp()).isTrue();
    }
}
