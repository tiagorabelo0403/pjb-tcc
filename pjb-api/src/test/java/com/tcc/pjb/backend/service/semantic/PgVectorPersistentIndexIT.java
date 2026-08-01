package com.tcc.pjb.backend.service.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbFlowItBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Prova end-to-end que {@link PgVectorPersistentIndex} substitui o {@link InMemoryCosineVectorIndex}
 * quando {@code pjb.ai.vector.mode=pgvector}, e que ingest + busca funcionam contra pgvector real
 * (Postgres via Testcontainers, extensao vector precompilada na imagem pgvector/pgvector:pg17,
 * schema criado pela migration V307).
 *
 * <p>Cenario: indexa 3 documentos com vetores conhecidos, busca com vetor identico ao segundo,
 * valida que o segundo aparece com score maior que os outros e que o filtro de metadata funciona.
 */
@TestPropertySource(properties = "pjb.ai.vector.mode=pgvector")
// Nao sobrescreve target-dimension: a coluna vector(1536) do schema V307 e fixa e o
// PgVectorPersistentIndex faz padding com zeros do lado da aplicacao para vetores menores
// (mantendo ortogonalidade e normalizacao). Os vetores de 8 elementos abaixo sao paddados
// para 1536 pelo adjustDimension antes do INSERT — mudanca de comportamento zero para as
// assertivas de ranking, e o teste passa a exercitar o mesmo caminho de dimensao usado em
// producao (default 1536, o mesmo do text-embedding-3-small).
class PgVectorPersistentIndexIT extends PjbFlowItBase {

    @Autowired
    VectorIndex vectorIndex;

    @Test
    void bootstrapAtivaAImplementacaoPgvectorEmVezDaInMemory() {
        assertThat(vectorIndex).isInstanceOf(PgVectorPersistentIndex.class);
    }

    @Test
    void indexarESearchAgainstRealPgvector_retornaMatchExatoComMaiorScore() {
        vectorIndex.upsert("prec-1",
                new EmbeddingVector(new float[]{1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f}),
                Map.of("ramo", "PENAL", "titulo", "Precedente A"));
        vectorIndex.upsert("prec-2",
                new EmbeddingVector(new float[]{0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f}),
                Map.of("ramo", "CIVEL", "titulo", "Precedente B"));
        vectorIndex.upsert("prec-3",
                new EmbeddingVector(new float[]{0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f}),
                Map.of("ramo", "TRABALHISTA", "titulo", "Precedente C"));

        List<VectorSearchHit> hits = vectorIndex.search(
                new EmbeddingVector(new float[]{0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f}), 3, Map.of());

        assertThat(hits).hasSize(3);
        assertThat(hits.get(0).id()).isEqualTo("prec-2");
        assertThat(hits.get(0).score()).isGreaterThan(hits.get(1).score());
        assertThat(hits.get(0).score()).isGreaterThan(0.9f);
    }

    @Test
    void filtroMetadataRealmenteFiltra_soRetornaDocumentosDoRamoSolicitado() {
        vectorIndex.upsert("prec-p1",
                new EmbeddingVector(new float[]{1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f}),
                Map.of("ramo", "PENAL"));
        vectorIndex.upsert("prec-c1",
                new EmbeddingVector(new float[]{1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f}),
                Map.of("ramo", "CIVEL"));

        List<VectorSearchHit> hits = vectorIndex.search(
                new EmbeddingVector(new float[]{1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f}), 5,
                Map.of("ramo", "PENAL"));

        assertThat(hits).extracting(VectorSearchHit::id).contains("prec-p1").doesNotContain("prec-c1");
    }

    @Test
    void upsertMesmoIdSubstituiConteudo() {
        EmbeddingVector v1 = new EmbeddingVector(new float[]{1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f});
        EmbeddingVector v2 = new EmbeddingVector(new float[]{0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f});
        vectorIndex.upsert("prec-mut", v1, Map.of("ramo", "PENAL"));
        vectorIndex.upsert("prec-mut", v2, Map.of("ramo", "PENAL"));

        List<VectorSearchHit> hitsSameAsV2 = vectorIndex.search(v2, 5, Map.of());
        List<VectorSearchHit> hitsSameAsV1 = vectorIndex.search(v1, 5, Map.of());

        // depois do 2o upsert, prec-mut esta perto de v2, nao mais de v1
        assertThat(hitsSameAsV2.stream().anyMatch(h -> h.id().equals("prec-mut") && h.score() > 0.9f)).isTrue();
        assertThat(hitsSameAsV1.stream().anyMatch(h -> h.id().equals("prec-mut") && h.score() > 0.9f)).isFalse();
    }
}
