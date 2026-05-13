package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoVisualIdentityRequest;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoPayloadHardeningServiceTest {

    @Test
    void deveCanonizarCamposCriticosERemoverBrandingInseguro() {
        PeticionamentoPayloadHardeningService service = new PeticionamentoPayloadHardeningService();
        PeticionamentoVisualIdentityRequest identidade = new PeticionamentoVisualIdentityRequest();
        identidade.setNomeExibicao("  Escritório   Atlas\u0000  ");
        identidade.setBrasaoOuLogomarcaUri("javascript:alert(1)");
        identidade.setPaletaPrimaria("blue");
        identidade.setCabecalhoLivre("  CABEÇALHO\n\n\nLIVRE ");

        LinkedHashMap<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("tribunalCodigo", "  TJCE  ");
        ctx.put("chave invalida", "ignorar");
        ctx.put("nested", java.util.Map.of("comarca", "  Morada Nova ", "ruim", java.util.List.of(" a ", "b ")));

        PeticionamentoSessaoRequest request = PeticionamentoSessaoRequest.builder()
                .tituloCaso("  Ação   de   indenização\u0000  ")
                .parteAutora("  Maria   Letícia  ")
                .textoPeticaoLivre("x".repeat(130000))
                .ufFato("ce")
                .ctx(ctx)
                .documentosAnexados(List.of("  peticao.pdf  ", "peticao.pdf", " comprovante  "))
                .enderecoAutor(PeticionamentoEnderecoRequest.builder().cep("62.940-000").uf("ce").build())
                .identidadeVisual(identidade)
                .build();

        var hardened = service.harden(request);

        assertEquals("Ação de indenização", hardened.request().getTituloCaso());
        assertEquals("CE", hardened.request().getUfFato());
        assertEquals("62940000", hardened.request().getEnderecoAutor().getCep());
        assertEquals("CE", hardened.request().getEnderecoAutor().getUf());
        assertEquals(2, hardened.request().getDocumentosAnexados().size());
        assertNull(hardened.request().getIdentidadeVisual().getBrasaoOuLogomarcaUri());
        assertNull(hardened.request().getIdentidadeVisual().getPaletaPrimaria());
        assertEquals("Escritório Atlas", hardened.request().getIdentidadeVisual().getNomeExibicao());
        assertTrue(hardened.request().getTextoPeticaoLivre().length() <= 120000);
        assertTrue(hardened.request().getCtx().containsKey("tribunalCodigo"));
        assertFalse(hardened.request().getCtx().containsKey("chave invalida"));
        assertNotNull(hardened.fingerprint());
        assertFalse(hardened.diagnostics().isEmpty());
    }

    @Test
    void deveAceitarUriConfiavelEProduzirMetadataEstavel() {
        PeticionamentoPayloadHardeningService service = new PeticionamentoPayloadHardeningService();
        PeticionamentoVisualIdentityRequest identidade = new PeticionamentoVisualIdentityRequest();
        identidade.setBrasaoOuLogomarcaUri("https://cdn.exemplo.justica/logo.png");
        identidade.setPaletaPrimaria("#0047AB");
        identidade.setPaletaSecundaria("#FFFFFF");
        identidade.setExibirBrasaoOuLogomarca(Boolean.TRUE);

        PeticionamentoSessaoRequest request = PeticionamentoSessaoRequest.builder()
                .modo("hibrido")
                .tituloCaso("Mandado de Segurança")
                .identidadeVisual(identidade)
                .build();

        var hardened = service.harden(request);

        assertEquals("https://cdn.exemplo.justica/logo.png", hardened.request().getIdentidadeVisual().getBrasaoOuLogomarcaUri());
        assertEquals("#0047AB", hardened.request().getIdentidadeVisual().getPaletaPrimaria());
        assertEquals("#FFFFFF", hardened.request().getIdentidadeVisual().getPaletaSecundaria());
        assertEquals("PETICIONAMENTO_INPUT_HARDENING_V2", hardened.metadata().get("profile"));
        assertEquals(hardened.fingerprint(), hardened.metadata().get("fingerprint"));
        assertTrue(hardened.metadata().containsKey("brandingEnabled"));
    }
}
