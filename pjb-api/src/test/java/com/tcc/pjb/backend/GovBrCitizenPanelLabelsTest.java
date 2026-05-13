package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.cidadao.govbr.GovBrCitizenPanelLabels;
import org.junit.jupiter.api.Test;

class GovBrCitizenPanelLabelsTest {

    @Test
    void deveResolverRotulosECoresCentralizados() {
        assertEquals("PJe", GovBrCitizenPanelLabels.sourceLabel("PJE"));
        assertEquals("eproc", GovBrCitizenPanelLabels.sourceLabel("eproc"));
        assertEquals("Réu", GovBrCitizenPanelLabels.roleLabel(PapelProcessualNacional.REU));
        assertEquals("VERMELHO_PENAL", GovBrCitizenPanelLabels.colorToken("PENAL_ORDINARIO", RamoDireito.PENAL));
        assertEquals("Família", GovBrCitizenPanelLabels.colorLabel("VINHO_FAMILIA"));
    }
}
