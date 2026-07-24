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
    void enumIsJusPostulandiCobreOsDoisFundamentosLegais() {
        assertTrue(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA.isJusPostulandi());
        assertTrue(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO.isJusPostulandi());
        assertFalse(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA.isJusPostulandi());
    }

    @Test
    void fromStringReconheceAliasesDoJusPostulandiDoJuizado() {
        assertEquals(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO, InstrumentoRepresentacaoProcessual.fromString("jus_postulandi_jec"));
        assertEquals(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO, InstrumentoRepresentacaoProcessual.fromString("JUS_POSTULANDI_LEI_9099"));
    }
}
