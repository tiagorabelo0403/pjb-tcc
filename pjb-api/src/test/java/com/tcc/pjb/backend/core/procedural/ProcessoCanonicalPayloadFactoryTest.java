package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessoCanonicalPayloadFactoryTest {

    @Test
    void deveMontarPayloadCanonicoImutavelComDadosDoProcesso() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setClasseProcessual("Mandado de Segurança");
        processo.setAssunto("Direito líquido e certo");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setMateria(MateriaJurisdicao.CIVIL);
        processo.setRito(RitoProcessual.ESPECIAL_MANDADO_SEGURANCA);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setValorCausa(new BigDecimal("12000.00"));

        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setNome("1ª Vara Cível de Fortaleza");
        jurisdicao.setSigla("TJCE");
        jurisdicao.setComarca("Fortaleza");
        jurisdicao.setEstado("CE");
        jurisdicao.setTipo(TipoJurisdicao.ESTADUAL);
        jurisdicao.setGrau(GrauJurisdicao.PRIMEIRO_GRAU);
        processo.setJurisdicao(jurisdicao);

        Map<String, Object> payload = ProcessoCanonicalPayloadFactory.fromProcesso(processo, "pedido liminar");

        assertEquals("ESPECIAL_MANDADO_SEGURANCA", payload.get("rito"));
        assertEquals("CIVIL", payload.get("ramoDireito"));
        assertEquals("Mandado de Segurança", payload.get("classeProcessual"));
        assertEquals("TJCE", payload.get("tribunalCodigo"));
        assertEquals("CE", payload.get("uf"));
        assertEquals("pedido liminar", payload.get("resumo"));
        assertTrue(payload.containsKey("valorCausa"));
    }
}
