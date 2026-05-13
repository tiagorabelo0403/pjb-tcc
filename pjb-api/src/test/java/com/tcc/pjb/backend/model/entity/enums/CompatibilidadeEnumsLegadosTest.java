package com.tcc.pjb.backend.model.entity.enums;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilidadeEnumsLegadosTest {

    @Test
    void ramoDireitoResolveAliasesLegados() {
        assertEquals(RamoDireito.CIVIL, RamoDireito.fromNullable("civel"));
        assertEquals(RamoDireito.ADMINISTRATIVO, RamoDireito.fromNullable("fazenda_publica"));
        assertEquals("FAZENDA", RamoDireito.ADMINISTRATIVO.verticalPrincipal());
    }

    @Test
    void faseProcessualResolveAliasesLegados() {
        assertEquals(FaseProcessual.CUMPRIMENTO_SENTENCA, FaseProcessual.fromString("cumprimento").orElseThrow());
        assertEquals(FaseProcessual.AUDIENCIA_CUSTODIA, FaseProcessual.fromString("custodia").orElseThrow());
    }

    @Test
    void tipoUsuarioResolvePapelArquitetural() {
        assertEquals(TipoUsuario.JUIZ, TipoUsuario.fromString("juiza"));
        assertEquals("MAGISTRATURA", TipoUsuario.JUIZ.papelArquitetural());
        assertTrue(TipoUsuario.ADVOGADO.isInstitucional());
        assertTrue(TipoUsuario.CIDADAO.isCidadaniaExterna());
    }
}
