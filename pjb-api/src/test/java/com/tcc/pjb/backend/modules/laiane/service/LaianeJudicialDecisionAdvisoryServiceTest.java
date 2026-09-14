package com.tcc.pjb.backend.modules.laiane.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.JudicialDecisionTemplateCode;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeAdvisoryMode;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudicialDecisionAdvisoryResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeSentencaDraftRequest;
import org.junit.jupiter.api.Test;

class LaianeJudicialDecisionAdvisoryServiceTest {

    private final LaianeJudicialDecisionAdvisoryService service = new LaianeJudicialDecisionAdvisoryService();

    @Test
    void acordoComDetalhesCompletosResultaSugestivoComMinutaDeDispositivo() {
        Processo processo = Processo.builder().id(1L).ramoDireito(RamoDireito.CIVIL).build();
        LaianeSentencaDraftRequest req = LaianeSentencaDraftRequest.builder()
                .processoId(1L)
                .fatos("As partes celebraram acordo de transacao com valor de R$ 5.000,00, pagamento em 3 parcelas, prazo de 90 dias.")
                .build();

        LaianeJudicialDecisionAdvisoryResponse resp = service.analyze(processo, req);

        assertThat(resp.templateCode()).isEqualTo(JudicialDecisionTemplateCode.HOMOLOGACAO_ACORDO);
        assertThat(resp.advisoryMode()).isEqualTo(LaianeAdvisoryMode.SUGESTIVO);
        assertThat(resp.dispositiveBase()).isNotBlank();
        assertThat(resp.pendingFacts()).isEmpty();
    }

    @Test
    void acordoSemDetalhesResultaRestritivoSemMinutaDeDispositivo() {
        Processo processo = Processo.builder().id(2L).ramoDireito(RamoDireito.CIVIL).build();
        LaianeSentencaDraftRequest req = LaianeSentencaDraftRequest.builder()
                .processoId(2L)
                .fatos("As partes celebraram acordo de transacao para por fim ao litigio.")
                .build();

        LaianeJudicialDecisionAdvisoryResponse resp = service.analyze(processo, req);

        assertThat(resp.advisoryMode()).isEqualTo(LaianeAdvisoryMode.RESTRITIVO);
        assertThat(resp.dispositiveBase()).isNull();
        assertThat(resp.pendingFacts()).isNotEmpty();
        assertThat(resp.reasoningChecklist()).isNotEmpty();
    }

    @Test
    void mariaDaPenhaSemVetorDeRiscoDetalhadoResultaRestritivo() {
        Processo processo = Processo.builder().id(3L).build();
        LaianeSentencaDraftRequest req = LaianeSentencaDraftRequest.builder()
                .processoId(3L)
                .fatos("Caso de violencia domestica contra a mulher, medida protetiva requerida pela ofendida.")
                .build();

        LaianeJudicialDecisionAdvisoryResponse resp = service.analyze(processo, req);

        assertThat(resp.templateCode()).isEqualTo(JudicialDecisionTemplateCode.MEDIDA_PROTETIVA_URGENTE_MARIA_DA_PENHA);
        assertThat(resp.advisoryMode()).isEqualTo(LaianeAdvisoryMode.RESTRITIVO);
        assertThat(resp.dispositiveBase()).isNull();
    }

    @Test
    void mariaDaPenhaComVetorDeRiscoDetalhadoResultaSugestivo() {
        Processo processo = Processo.builder().id(4L).build();
        LaianeSentencaDraftRequest req = LaianeSentencaDraftRequest.builder()
                .processoId(4L)
                .fatos("Violencia domestica; ofendida relata ameaca com arma e perseguicao reiterada pelo agressor.")
                .build();

        LaianeJudicialDecisionAdvisoryResponse resp = service.analyze(processo, req);

        assertThat(resp.advisoryMode()).isEqualTo(LaianeAdvisoryMode.SUGESTIVO);
        assertThat(resp.dispositiveBase()).isNotBlank();
    }

    @Test
    void casoSemPadraoReconhecidoResultaBloqueadorSemMinuta() {
        Processo processo = Processo.builder().id(5L).build();
        LaianeSentencaDraftRequest req = LaianeSentencaDraftRequest.builder()
                .processoId(5L)
                .fatos("Discussao contratual atipica sem enquadramento padronizado.")
                .build();

        LaianeJudicialDecisionAdvisoryResponse resp = service.analyze(processo, req);

        assertThat(resp.templateCode()).isEqualTo(JudicialDecisionTemplateCode.ASSISTENCIA_DECISORIA_GENERICA);
        assertThat(resp.advisoryMode()).isEqualTo(LaianeAdvisoryMode.BLOQUEADOR);
        assertThat(resp.dispositiveBase()).isNull();
    }

    @Test
    void reviewRequiredEPublicationLockedPermanecemSempreVerdadeiroEmTodosOsModos() {
        Processo processo = Processo.builder().id(6L).build();

        LaianeJudicialDecisionAdvisoryResponse sugestivo = service.analyze(processo,
                LaianeSentencaDraftRequest.builder().processoId(6L)
                        .fatos("Reconhecimento da procedencia do pedido pela parte re.").build());
        LaianeJudicialDecisionAdvisoryResponse restritivo = service.analyze(processo,
                LaianeSentencaDraftRequest.builder().processoId(6L)
                        .fatos("Desistencia da acao apresentada pela parte autora.").build());
        LaianeJudicialDecisionAdvisoryResponse bloqueador = service.analyze(processo,
                LaianeSentencaDraftRequest.builder().processoId(6L)
                        .fatos("Materia atipica sem padrao reconhecido pelo motor.").build());

        for (LaianeJudicialDecisionAdvisoryResponse resp : java.util.List.of(sugestivo, restritivo, bloqueador)) {
            assertThat(resp.reviewRequired()).isTrue();
            assertThat(resp.publicationLocked()).isTrue();
        }
    }
}
