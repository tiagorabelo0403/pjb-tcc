package com.tcc.pjb.backend.service.rito;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.secretariat.acceleration.SecretariatQueuePriorityPolicy;
import org.junit.jupiter.api.Test;

class RitoUrgenciaPriorityPolicyTest {

    private final RitoUrgenciaPriorityPolicy policy = new RitoUrgenciaPriorityPolicy();

    @Test
    void mariaDaPenha_elevaPrioridadeParaMaxima_mesmoQuandoBaseEraBaixa() {
        assertThat(policy.prioridade(RitoProcessual.PENAL_MARIA_DA_PENHA, 3))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_MAXIMA);
    }

    @Test
    void habeasCorpus_elevaPrioridadeParaMaxima() {
        assertThat(policy.prioridade(RitoProcessual.ESPECIAL_HABEAS_CORPUS, 3))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_MAXIMA);
        assertThat(policy.prioridade(RitoProcessual.PENAL_HABEAS_CORPUS_PREVENTIVO, 3))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_MAXIMA);
        assertThat(policy.prioridade(RitoProcessual.MILITAR_HABEAS_CORPUS_MILITAR, 3))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_MAXIMA);
    }

    @Test
    void tutelaUrgente_elevaPrioridadeParaAlta_masNaoParaMaxima() {
        assertThat(policy.prioridade(RitoProcessual.CIVIL_TUTELA_URGENTE, 3))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_ALTA);
        assertThat(policy.prioridade(RitoProcessual.TRABALHISTA_TUTELA_CAUTELAR, 5))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_ALTA);
    }

    @Test
    void ecaInfracional_elevaPrioridadeParaAlta() {
        assertThat(policy.prioridade(RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL, 3))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_ALTA);
        assertThat(policy.prioridade(RitoProcessual.PENAL_ECA_INFRACIONAL, 3))
                .isEqualTo(RitoUrgenciaPriorityPolicy.PRIORIDADE_ALTA);
    }

    @Test
    void nuncaRebaixaPrioridadeJaMaisCriticaQueOTetoDoNivel() {
        assertThat(policy.prioridade(RitoProcessual.PENAL_MARIA_DA_PENHA, 1)).isEqualTo(1);
        assertThat(policy.prioridade(RitoProcessual.CIVIL_TUTELA_URGENTE, 1)).isEqualTo(1);
    }

    @Test
    void ritoComumNaoUrgente_mantemPrioridadeBaseInalterada() {
        assertThat(policy.prioridade(RitoProcessual.COMUM_ORDINARIO, 3)).isEqualTo(3);
    }

    @Test
    void ritoNulo_mantemPrioridadeBaseInalterada() {
        assertThat(policy.prioridade(null, 3)).isEqualTo(3);
    }

    @Test
    void mariaDaPenha_retornaBaseLegalComPrazoDe48h() {
        assertThat(policy.baseLegalAdicional(RitoProcessual.PENAL_MARIA_DA_PENHA))
                .contains("Lei 11.340/06")
                .contains("48h");
    }

    @Test
    void habeasCorpus_retornaBaseLegalConstitucional() {
        assertThat(policy.baseLegalAdicional(RitoProcessual.ESPECIAL_HABEAS_CORPUS))
                .contains("CF art. 5º, LXVIII");
    }

    @Test
    void tutelaUrgente_retornaBaseLegalDoCpc() {
        assertThat(policy.baseLegalAdicional(RitoProcessual.CIVIL_TUTELA_URGENTE))
                .contains("CPC art. 300");
    }

    @Test
    void ecaInfracional_retornaBaseLegalComPrazoDe45Dias() {
        assertThat(policy.baseLegalAdicional(RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL))
                .contains("ECA")
                .contains("45 dias");
    }

    @Test
    void ritoComumNaoUrgente_naoRetornaBaseLegalAdicional() {
        assertThat(policy.baseLegalAdicional(RitoProcessual.COMUM_ORDINARIO)).isNull();
    }

    @Test
    void ritoNulo_naoRetornaBaseLegalAdicional() {
        assertThat(policy.baseLegalAdicional(null)).isNull();
    }

    @Test
    void mariaDaPenha_geraTagsSigiloETutela() {
        assertThat(policy.tagsSecretariat(RitoProcessual.PENAL_MARIA_DA_PENHA))
                .contains(SecretariatQueuePriorityPolicy.TAG_PRIORIDADE_LEGAL, SecretariatQueuePriorityPolicy.TAG_SIGILO);
    }

    @Test
    void habeasCorpus_geraTagPrioridadeLegalSemSigilo() {
        assertThat(policy.tagsSecretariat(RitoProcessual.ESPECIAL_HABEAS_CORPUS))
                .contains(SecretariatQueuePriorityPolicy.TAG_PRIORIDADE_LEGAL)
                .doesNotContain(SecretariatQueuePriorityPolicy.TAG_SIGILO);
    }

    @Test
    void civilTutelaUrgente_geraTagTutelaETagPrioridadeLegal() {
        assertThat(policy.tagsSecretariat(RitoProcessual.CIVIL_TUTELA_URGENTE))
                .contains(SecretariatQueuePriorityPolicy.TAG_TUTELA, SecretariatQueuePriorityPolicy.TAG_PRIORIDADE_LEGAL);
    }

    @Test
    void ritoComumNaoUrgente_naoGeraNenhumaTag() {
        assertThat(policy.tagsSecretariat(RitoProcessual.COMUM_ORDINARIO)).isEmpty();
    }

    @Test
    void ritoNulo_naoGeraNenhumaTag() {
        assertThat(policy.tagsSecretariat(null)).isEmpty();
    }
}
