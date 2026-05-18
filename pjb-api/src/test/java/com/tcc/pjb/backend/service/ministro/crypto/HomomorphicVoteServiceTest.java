package com.tcc.pjb.backend.service.ministro.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.ministro.PlenarioVirtualVoto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HomomorphicVoteServiceTest {

    private HomomorphicVoteService service;

    @BeforeEach
    void setUp() {
        service = new HomomorphicVoteService(new ObjectMapper());
    }

    @Test
    void options_retornaQuatroOpcoes() {
        List<String> opts = service.options();
        assertThat(opts).hasSize(4)
                .contains("ACOMPANHA_RELATOR", "DIVERGE", "PARCIAL", "ABSTENCAO");
    }

    @Test
    void sealBallot_retornaBallotNaoNulo() {
        HomomorphicVoteService.HomomorphicBallot ballot =
                service.sealBallot("sessao-001", 1L, "ACOMPANHA_RELATOR");
        assertThat(ballot).isNotNull();
        assertThat(ballot.ballotBlob()).isNotBlank();
        assertThat(ballot.commitmentHash()).isNotBlank();
        assertThat(ballot.proofHash()).isNotBlank();
    }

    @Test
    void sealBallot_commitmentEProofHashSaoDiferentes() {
        HomomorphicVoteService.HomomorphicBallot ballot =
                service.sealBallot("sessao-002", 2L, "DIVERGE");
        assertThat(ballot.commitmentHash()).isNotEqualTo(ballot.proofHash());
    }

    @Test
    void sealBallot_opcaoDesconhecidaDefaultsParaAbstencao() {
        HomomorphicVoteService.HomomorphicBallot ballot =
                service.sealBallot("sessao-003", 3L, "OPCAO_INVALIDA");
        assertThat(ballot.ballotBlob()).isNotBlank();
        assertThat(ballot.commitmentHash()).isNotBlank();
    }

    @Test
    void openAggregate_listaVaziaRetornaTallyZero() {
        HomomorphicVoteService.AggregateView result =
                service.openAggregate("sessao-vazia", List.of());
        assertThat(result.votosConsiderados()).isEqualTo(0);
        assertThat(result.tally()).containsEntry("ACOMPANHA_RELATOR", 0);
        assertThat(result.tally()).containsEntry("DIVERGE", 0);
        assertThat(result.tally()).containsEntry("ABSTENCAO", 0);
    }

    @Test
    void openAggregate_votoBlobVazioIgnorado() {
        PlenarioVirtualVoto votoVazio = new PlenarioVirtualVoto();
        votoVazio.setHomomorphicTallyBlob("   ");
        HomomorphicVoteService.AggregateView result =
                service.openAggregate("sessao-blank", List.of(votoVazio));
        assertThat(result.votosConsiderados()).isEqualTo(0);
    }

    @Test
    void sealEOpenAggregate_roundTripUmVoto() {
        String sessao = "sessao-roundtrip";
        HomomorphicVoteService.HomomorphicBallot ballot =
                service.sealBallot(sessao, 10L, "ACOMPANHA_RELATOR");

        PlenarioVirtualVoto voto = new PlenarioVirtualVoto();
        voto.setHomomorphicTallyBlob(ballot.ballotBlob());

        HomomorphicVoteService.AggregateView result =
                service.openAggregate(sessao, List.of(voto));

        assertThat(result.votosConsiderados()).isEqualTo(1);
        assertThat(result.tally().get("ACOMPANHA_RELATOR")).isEqualTo(1);
        assertThat(result.tally().get("DIVERGE")).isEqualTo(0);
        assertThat(result.tally().get("PARCIAL")).isEqualTo(0);
        assertThat(result.tally().get("ABSTENCAO")).isEqualTo(0);
    }

    @Test
    void sealEOpenAggregate_doisVotosOpcoesDiferentes() {
        String sessao = "sessao-dois-votos";
        PlenarioVirtualVoto v1 = new PlenarioVirtualVoto();
        v1.setHomomorphicTallyBlob(service.sealBallot(sessao, 1L, "ACOMPANHA_RELATOR").ballotBlob());
        PlenarioVirtualVoto v2 = new PlenarioVirtualVoto();
        v2.setHomomorphicTallyBlob(service.sealBallot(sessao, 2L, "DIVERGE").ballotBlob());

        HomomorphicVoteService.AggregateView result =
                service.openAggregate(sessao, List.of(v1, v2));

        assertThat(result.votosConsiderados()).isEqualTo(2);
        assertThat(result.tally().get("ACOMPANHA_RELATOR")).isEqualTo(1);
        assertThat(result.tally().get("DIVERGE")).isEqualTo(1);
        assertThat(result.tally().get("PARCIAL")).isEqualTo(0);
    }

    @Test
    void openAggregate_aggregateHashNaoVazio() {
        HomomorphicVoteService.AggregateView result =
                service.openAggregate("sessao-hash", List.of());
        assertThat(result.aggregateHash()).isNotBlank();
    }
}
