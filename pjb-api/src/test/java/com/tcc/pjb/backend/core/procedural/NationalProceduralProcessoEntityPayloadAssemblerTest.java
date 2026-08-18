package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalProceduralProcessoEntityPayloadAssemblerTest {

    private final NationalProceduralProcessoEntityPayloadAssembler assembler =
            new NationalProceduralProcessoEntityPayloadAssembler();

    @Test
    void fronteira2_anexosSemTipoDeclarado_naoDeveAdicionarChaveDocumentosTipados() {
        Attachment semTipo = Attachment.builder().name("petição.pdf").build();

        LinkedHashMap<String, Object> payload = assembler.assemble(null, null, List.of(semTipo));

        assertFalse(payload.containsKey("documentosTipados"),
                "Chave 'documentosTipados' não deve ser adicionada quando todos os Attachments têm tipoDocumento=null " +
                "— caso contrário, lista vazia ativa Fronteira 2 e bloqueia routing para callers sem canal tipado");
    }

    @Test
    void canalTipado_anexosComTipoDeclarado_deveAdicionarChaveComTiposNaoNulos() {
        Attachment tipado = Attachment.builder().name("peticao.pdf").tipoDocumento(TipoDocumento.PETICAO_INICIAL).build();
        Attachment semTipo = Attachment.builder().name("rascunho.pdf").build();

        LinkedHashMap<String, Object> payload = assembler.assemble(null, null, List.of(tipado, semTipo));

        assertTrue(payload.containsKey("documentosTipados"),
                "Chave 'documentosTipados' deve ser adicionada quando há pelo menos um tipoDocumento não-nulo");
        @SuppressWarnings("unchecked")
        List<String> tipados = (List<String>) payload.get("documentosTipados");
        assertEquals(List.of("PETICAO_INICIAL"), tipados,
                "Somente TipoDocumento não-nulos devem compor a lista — null filtrado");
    }

    @Test
    void canalLegado_listaVazia_naoDeveAdicionarChaveDocumentosTipados() {
        LinkedHashMap<String, Object> payload = assembler.assemble(null, null, List.of());

        assertFalse(payload.containsKey("documentosTipados"),
                "Lista de anexos vazia não deve adicionar chave — caller sem Attachment usa canal legado");
    }
}
