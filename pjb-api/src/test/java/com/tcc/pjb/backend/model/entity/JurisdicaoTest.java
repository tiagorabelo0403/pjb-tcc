package com.tcc.pjb.backend.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.NaturezaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class JurisdicaoTest {

    @Test
    void deveExporAccessorsExplicitosSemLombok() {
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setCodigo("TJMG-BH-1");
        jurisdicao.setSigla("TJMG");
        jurisdicao.setNome("Tribunal de Justiça de Minas Gerais");
        jurisdicao.setTipo(TipoJurisdicao.ESTADUAL);
        jurisdicao.setNatureza(NaturezaJurisdicao.CONTENCIOSA);
        jurisdicao.setGrau(GrauJurisdicao.PRIMEIRO_GRAU);
        jurisdicao.setEsfera(EsferaJurisdicao.JUSTICA_ESTADUAL);
        jurisdicao.setMateria(MateriaJurisdicao.CIVIL);
        jurisdicao.setPermiteJuizadoEspecial(Boolean.TRUE);
        jurisdicao.setValorMinimoCausa(BigDecimal.ZERO);
        jurisdicao.setTetoValorCausa(new BigDecimal("40000.00"));

        assertEquals("TJMG-BH-1", jurisdicao.getCodigo());
        assertEquals("TJMG", jurisdicao.getSigla());
        assertEquals("Tribunal de Justiça de Minas Gerais", jurisdicao.getNome());
        assertEquals(TipoJurisdicao.ESTADUAL, jurisdicao.getTipo());
        assertEquals(NaturezaJurisdicao.CONTENCIOSA, jurisdicao.getNatureza());
        assertEquals(GrauJurisdicao.PRIMEIRO_GRAU, jurisdicao.getGrau());
        assertEquals(EsferaJurisdicao.JUSTICA_ESTADUAL, jurisdicao.getEsfera());
        assertEquals(MateriaJurisdicao.CIVIL, jurisdicao.getMateria());
        assertTrue(jurisdicao.aceitaJuizadoEspecial());
        assertTrue(jurisdicao.valorCausaDentroDosLimites(new BigDecimal("1500.00")));
        assertFalse(jurisdicao.valorCausaDentroDosLimites(new BigDecimal("50000.00")));
    }
}
