package com.tcc.pjb.backend.service.processual.representacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class RepresentacaoProcessualPolicyServiceTest {

    private final RepresentacaoProcessualPolicyService service = new RepresentacaoProcessualPolicyService();

    @Test
    void cidadaoEmJuizadoEspecialCivelResolveJusPostulandiSemExigirProcuracao() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "CIVIL", "JUIZADO_ESPECIAL_CIVEL", "TJCE", TipoUsuario.CIDADAO,
                null, null, null, false, false, null, null);

        assertEquals("JUS_POSTULANDI_JUIZADO", policy.resolvedInstrument());
        assertTrue(policy.regularidadeSuficiente());
        assertFalse(policy.exigeProcuracaoFormal());
        assertTrue(policy.documentosBase().stream().noneMatch(d -> d.contains("Procuração geral para o foro")));
        assertTrue(policy.fundamentosLegais().stream().anyMatch(f -> f.contains("Lei 9.099/95, art. 9º")));
        assertTrue(policy.atosPermitidos().stream().anyMatch(a -> a.contains("Juizado Especial Cível")));
        assertEquals("AUTORREPRESENTACAO_JUIZADO_ESPECIAL", policy.regimePostulacao());
        assertTrue(service.representacaoSuficiente(policy, false, false));
    }

    @Test
    void cidadaoEmRitoTrabalhistaContinuaResolvendoJusPostulandiTrabalhista() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "TRABALHISTA", "TRABALHISTA_ORDINARIO", "TRT7", TipoUsuario.CIDADAO,
                null, null, null, false, false, null, null);

        assertEquals("JUS_POSTULANDI_TRABALHISTA", policy.resolvedInstrument());
        assertTrue(policy.regularidadeSuficiente());
        assertFalse(policy.exigeProcuracaoFormal());
        assertEquals("AUTORREPRESENTACAO_TRABALHISTA_EXCEPCIONAL", policy.regimePostulacao());
    }

    @Test
    void advogadoEmJuizadoEspecialCivelContinuaExigindoMandatoAdJudicia() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "CIVIL", "JUIZADO_ESPECIAL_CIVEL", "TJCE", TipoUsuario.ADVOGADO,
                null, null, null, false, false, null, null);

        assertEquals("MANDATO_AD_JUDICIA", policy.resolvedInstrument());
        assertTrue(policy.exigeProcuracaoFormal());
        assertTrue(policy.documentosBase().stream().anyMatch(d -> d.contains("Procuração geral para o foro")));
        assertFalse(service.representacaoSuficiente(policy, false, false));
    }

    @Test
    void instrumentoJusPostulandiJuizadoRequisitadoPorAdvogadoEIrregular() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "CIVIL", "JUIZADO_ESPECIAL_CIVEL", "TJCE", TipoUsuario.ADVOGADO,
                "JUS_POSTULANDI_JUIZADO", null, null, false, false, null, null);

        assertEquals("JUS_POSTULANDI_JUIZADO", policy.resolvedInstrument());
        assertFalse(policy.regularidadeSuficiente());
        assertFalse(service.representacaoSuficiente(policy, false, false));
    }

    @Test
    void instrumentoJusPostulandiJuizadoResolvidoForaDoRitoEIrregular() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "CIVIL", "COMUM_ORDINARIO", "TJCE", TipoUsuario.CIDADAO,
                "JUS_POSTULANDI_JUIZADO", null, null, false, false, null, null);

        assertEquals("JUS_POSTULANDI_JUIZADO", policy.resolvedInstrument());
        assertFalse(policy.regularidadeSuficiente());
    }

    @Test
    void cidadaoEmJuizadoEspecialFederalResolveJusPostulandiProprio() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "PREVIDENCIARIO", "JUIZADO_ESPECIAL_FEDERAL", "TRF5", TipoUsuario.CIDADAO,
                null, null, null, false, false, null, null);

        assertEquals("JUS_POSTULANDI_JEF", policy.resolvedInstrument());
        assertTrue(policy.regularidadeSuficiente());
        assertFalse(policy.exigeProcuracaoFormal());
        assertTrue(policy.documentosBase().stream().noneMatch(d -> d.contains("Procuração geral para o foro")));
        assertTrue(policy.fundamentosLegais().stream().anyMatch(f -> f.contains("Lei 10.259/2001, art. 10")));
        assertTrue(policy.fundamentosLegais().stream().noneMatch(f -> f.contains("Lei 9.099/95, art. 9º")));
        assertEquals("AUTORREPRESENTACAO_JUIZADO_ESPECIAL_FEDERAL", policy.regimePostulacao());
        assertTrue(service.representacaoSuficiente(policy, false, false));
    }

    @Test
    void cidadaoEmPrevidenciarioJefResolveJusPostulandiProprio() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "PREVIDENCIARIO", "PREVIDENCIARIO_JEF", "TRF5", TipoUsuario.CIDADAO,
                null, null, null, false, false, null, null);

        assertEquals("JUS_POSTULANDI_JEF", policy.resolvedInstrument());
        assertTrue(policy.regularidadeSuficiente());
        assertTrue(service.representacaoSuficiente(policy, false, false));
    }

    @Test
    void advogadoEmJuizadoEspecialFederalContinuaExigindoMandatoAdJudicia() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "PREVIDENCIARIO", "JUIZADO_ESPECIAL_FEDERAL", "TRF5", TipoUsuario.ADVOGADO,
                null, null, null, false, false, null, null);

        assertEquals("MANDATO_AD_JUDICIA", policy.resolvedInstrument());
        assertTrue(policy.exigeProcuracaoFormal());
        assertFalse(service.representacaoSuficiente(policy, false, false));
    }

    @Test
    void instrumentoJusPostulandiJefResolvidoForaDoRitoEIrregular() {
        RepresentacaoProcessualPolicyResponse policy = service.resolve(
                "CIVIL", "JUIZADO_ESPECIAL_CIVEL", "TJCE", TipoUsuario.CIDADAO,
                "JUS_POSTULANDI_JEF", null, null, false, false, null, null);

        assertEquals("JUS_POSTULANDI_JEF", policy.resolvedInstrument());
        assertFalse(policy.regularidadeSuficiente());
    }

    @Test
    void enumIsJusPostulandiCobreOsTresFundamentosLegais() {
        assertTrue(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA.isJusPostulandi());
        assertTrue(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO.isJusPostulandi());
        assertTrue(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JEF.isJusPostulandi());
        assertFalse(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA.isJusPostulandi());
    }

    @Test
    void fromStringReconheceAliasesDoJusPostulandiDoJuizado() {
        assertEquals(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO, InstrumentoRepresentacaoProcessual.fromString("jus_postulandi_jec"));
        assertEquals(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO, InstrumentoRepresentacaoProcessual.fromString("JUS_POSTULANDI_LEI_9099"));
    }

    @Test
    void fromStringReconheceAliasesDoJusPostulandiFederal() {
        assertEquals(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JEF, InstrumentoRepresentacaoProcessual.fromString("jus_postulandi_jef"));
        assertEquals(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JEF, InstrumentoRepresentacaoProcessual.fromString("JUS_POSTULANDI_LEI_10259"));
    }
}
