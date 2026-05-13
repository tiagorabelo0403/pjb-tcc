package com.tcc.pjb.backend.service.operational.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.service.institutional.support.lane.InstitutionalSupportLaneResolver;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProceduralFormalCatalogServiceTest {

    private final ProceduralFormalCatalogService service = new ProceduralFormalCatalogService();

    @Test
    void shouldProjectRecursalAndPenalFormalCatalogForSecretariat() {
        SecretariatQueueItem item = SecretariatQueueItem.builder()
                .workItemId(31L)
                .processoId(301L)
                .inboxKey("SECRETARIA:2G:TJCE:CE:FORTALEZA:CAMARA")
                .queueCode("COLEGIADO_JURI_ACORDAO")
                .status("PENDENTE")
                .prioridade(1)
                .score(90)
                .titulo("Pauta colegiada do júri")
                .hearingSensitive(true)
                .blocking(true)
                .secrecyReviewRequired(true)
                .updatedAt(Instant.now())
                .createdAt(Instant.now())
                .rowVersion(1L)
                .build();
        Map<Long, Map<String, Object>> metadata = new LinkedHashMap<>();
        metadata.put(31L, Map.of("ramoDireito", RamoDireito.PENAL.name()));

        ProceduralFormalCatalogService.FormalCatalogProjection projection = service.resolveSecretariatCatalog(
                "SECRETARIA:2G:TJCE:CE:FORTALEZA:CAMARA",
                List.of(item),
                metadata
        );

        assertThat(projection.ritoAxis()).isEqualTo(RitoProcessual.TRIBUNAL_JURI.name());
        assertThat(projection.documents()).extracting(ProceduralFormalCatalogService.FormalDocument::documentCode)
                .contains("PAUTA_PACOTE_COLEGIADO", "COMUNICACAO_ACORDAO_BAIXA", "CERTIDAO_COMPARECIMENTO_PENAL");
    }

    @Test
    void shouldProjectInstitutionalCatalogWithoutInvalidRitoEnum() {
        Processo processo = Processo.builder()
                .id(401L)
                .ramoDireito(RamoDireito.PENAL)
                .rito(RitoProcessual.TRIBUNAL_JURI)
                .faseAtual(FaseProcessual.RECURSAL)
                .nivelSigilo(NivelSigilo.SIGILO_N2)
                .build();
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = new InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot(
                "MINISTERIO_PUBLICO",
                "Secretaria institucional do Ministério Público",
                "ESTADUAL",
                "ESTADUAL",
                "TJCE",
                "CE",
                "FORTALEZA",
                "MP:ESTADUAL:TJCE",
                "/api/v1/forum/desks/self",
                "/api/v1/mp/painel",
                "/api/v1/institutional-support/MINISTERIO_PUBLICO/snapshot",
                "/api/v1/institutional-support/MINISTERIO_PUBLICO/credential-security",
                List.of("PROMOTOR"),
                List.of("MALHA_INSTITUCIONAL"),
                List.of()
        );

        ProceduralFormalCatalogService.FormalCatalogProjection projection = service.resolveInstitutionalCatalog(
                "MINISTERIO_PUBLICO",
                lane,
                processo,
                List.of()
        );

        assertThat(projection.ritoAxis()).isEqualTo(RitoProcessual.TRIBUNAL_JURI.name());
        assertThat(projection.documents()).extracting(ProceduralFormalCatalogService.FormalDocument::documentCode)
                .contains("CERTIDAO_COMPARECIMENTO_PENAL");
    }
}
