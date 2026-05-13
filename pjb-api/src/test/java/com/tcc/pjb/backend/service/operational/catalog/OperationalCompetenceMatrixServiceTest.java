package com.tcc.pjb.backend.service.operational.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OperationalCompetenceMatrixServiceTest {

    private final OperationalCompetenceMatrixService service = new OperationalCompetenceMatrixService();

    @Test
    void shouldProjectSecretariatCompetenceMatrixWithCredentialAndAgendaRules() {
        SecretariatInstitutionalVisibilityService.ActorSecretariatScope actorScope =
                new SecretariatInstitutionalVisibilityService.ActorSecretariatScope(
                        "TJCE",
                        "PRIMEIRA_INSTANCIA",
                        "ESTADUAL",
                        "CE",
                        "FORTALEZA",
                        "1VARA",
                        "SEC_ESTADUAL",
                        true
                );
        SecretariatSpecializationResolver.SecretariatSpecializationProfile specialization =
                new SecretariatSpecializationResolver.SecretariatSpecializationProfile(
                        "SECRETARIA_PRIMEIRA_INSTANCIA_ESTADUAL",
                        "PRIMEIRA_INSTANCIA",
                        "ESTADUAL",
                        "PJB_ESTADUAL",
                        "PJB Estadual | Primeira Instância",
                        "pje-estadual-primeira",
                        "SEC_ESTADUAL",
                        "Secretaria Judicial Estadual",
                        "SECRETARIA:ESTADUAL:TJCE:CE:FORTALEZA:1VARA",
                        List.of("COLEGIADO", "TRABALHISTA"),
                        Map.of()
                );
        SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile profile =
                new SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile(
                        "SECRETARIA:ESTADUAL:TJCE:CE:FORTALEZA:1VARA",
                        actorScope,
                        specialization
                );
        ForumDeskPortfolioProfile portfolio = new ForumDeskPortfolioProfile(
                "TRIAGEM",
                "GABINETE",
                "AGENDA",
                "CUMPRIMENTO",
                "ESCALONAMENTO",
                "ASSISTENTE",
                "COORDENACAO",
                "REDISTRIBUICAO",
                "DASH_SECRETARIA",
                List.of("SECRETARIA"),
                new LinkedHashMap<>()
        );
        SecretariatQueueItem item = SecretariatQueueItem.builder()
                .workItemId(51L)
                .processoId(501L)
                .inboxKey("SECRETARIA:ESTADUAL:TJCE:CE:FORTALEZA:1VARA")
                .queueCode("AUDIENCIA_RECURSAL_SIGILO")
                .status("PENDENTE")
                .prioridade(1)
                .score(99)
                .titulo("Preparar audiência sigilosa")
                .hearingSensitive(true)
                .blocking(true)
                .secrecyReviewRequired(true)
                .escalationRequired(true)
                .updatedAt(Instant.now())
                .createdAt(Instant.now())
                .rowVersion(1L)
                .build();
        LinkedHashMap<Long, Map<String, Object>> metadata = new LinkedHashMap<>();
        metadata.put(51L, Map.of(
                "ritoProcessual", "TRIBUNAL_JURI",
                "faseProcessual", "RECURSAL",
                "ramoDireito", "PENAL",
                "nivelSigilo", "SIGILO_N2"
        ));

        OperationalCompetenceMatrixService.MatrixProjection projection = service.resolveSecretariat(
                "SECRETARIA:ESTADUAL:TJCE:CE:FORTALEZA:1VARA",
                profile,
                portfolio,
                List.of(item),
                metadata
        );

        assertThat(projection.rules()).extracting(OperationalCompetenceMatrixService.CompetenceRule::actCode)
                .contains("PREPARAR_MINUTA_OPERACIONAL", "CONFIRMAR_LOCAL_AUDIENCIA_SESSAO", "OPERAR_ATO_SIGILOSO");
        assertThat(projection.metrics()).containsEntry("hearingSensitive", true);
        assertThat(projection.warnings()).isNotEmpty();
    }
}
