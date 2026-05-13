package com.tcc.pjb.backend.core.procedural;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "CompetenceResolver", pactVersion = PactSpecVersion.V3)
class NationalProceduralRoutingCompetenceContractTest {

    @Pact(consumer = "NationalProceduralRouting")
    RequestResponsePact competenceResolvePact(PactDslWithProvider builder) {
        PactDslJsonBody requestBody = new PactDslJsonBody()
                .stringValue("textoCaso", "ação de obrigação de fazer com pedido de tutela")
                .stringValue("assunto", "saúde")
                .stringValue("classeProcessual", "procedimento comum")
                .stringValue("materia", "cível")
                .stringValue("uf", "SP")
                .stringValue("comarca", "São Paulo")
                .decimalType("valorCausa", 1000.0)
                .booleanType("envolveUniao", false)
                .booleanType("envolveAutarquiaFederal", false)
                .booleanType("envolveEmpresaPublicaFederal", false)
                .booleanType("envolveEstado", false)
                .booleanType("envolveMunicipio", false)
                .booleanType("envolveRelacaoTrabalho", false)
                .booleanType("envolveEleitoral", false)
                .booleanType("envolveMilitar", false);
        DslPart responseBody = new PactDslJsonBody()
                .uuid("requestId")
                .stringValue("generatedAt", "2026-04-11T12:00:00Z")
                .stringValue("tipoJusticaSugerida", "ESTADUAL")
                .stringValue("ritoSugerido", "PROCEDIMENTO_COMUM")
                .decimalType("confidence", 0.91)
                .array("reasons")
                    .stringValue("razão canônica")
                .closeArray()
                .array("legalBases")
                    .stringValue("CF/88, art. 5º")
                .closeArray()
                .object("debug")
                    .stringValue("source", "contract")
                .closeObject();
        return builder
                .given("competence resolver resolve rota cível SP")
                .uponReceiving("resolução de competência para caso cível em SP")
                .path("/api/v1/intelligence/competencia/resolve")
                .method("POST")
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body(requestBody)
                .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body(responseBody)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "competenceResolvePact")
    void competenceResolver_deveManterContratoDeResposta(MockServer mockServer) {
        CompetenceResolverContractClient pactClient = new CompetenceResolverContractClient(mockServer.getUrl());
        CompetenceResolveResponse response = pactClient.resolve(new CompetenceResolveRequest(
                "ação de obrigação de fazer com pedido de tutela",
                "saúde",
                "procedimento comum",
                "cível",
                "SP",
                "São Paulo",
                BigDecimal.valueOf(1000),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));
        assertThat(response).isNotNull();
        assertThat(response.tipoJusticaSugerida()).isEqualTo("ESTADUAL");
        assertThat(response.ritoSugerido()).isEqualTo("PROCEDIMENTO_COMUM");
        assertThat(response.confidence()).isPositive();
        assertThat(response.reasons()).isNotEmpty();
        assertThat(response.legalBases()).isNotEmpty();
        assertThat(response.debug()).containsEntry("source", "contract");
    }
}
