package com.tcc.pjb.backend.modules.acordo.domain;

import java.time.Instant;

public class AcordoProcessualStateMachine {

    public void requireMensagemPermitida(AcordoSessaoStatus status, boolean expirada, boolean participanteAceito) {
        requireSalaAtiva(status, expirada);
        requireParticipanteAceito(participanteAceito);
        if (!status.acceptsMessages()) {
            throw new AcordoDomainException("Mensagem bloqueada pelo estado da sala: " + status);
        }
    }

    public void requirePropostaPermitida(AcordoSessaoStatus status, boolean expirada, boolean participanteAceito, Instant validadeAte, Instant now) {
        requireSalaAtiva(status, expirada);
        requireParticipanteAceito(participanteAceito);
        if (!status.acceptsProposals()) {
            throw new AcordoDomainException("Proposta bloqueada pelo estado da sala: " + status);
        }
        if (validadeAte == null || !validadeAte.isAfter(now)) {
            throw new AcordoDomainException("Proposta formal exige validade futura.");
        }
    }

    public void requireGeracaoTermo(AcordoSessaoStatus status,
                                    boolean expirada,
                                    boolean participanteAceito,
                                    AcordoPropostaStatus propostaStatus,
                                    boolean propostaExpirada,
                                    boolean criadaPorIa,
                                    boolean revisadaPorHumano) {
        requireSalaAtiva(status, expirada);
        requireParticipanteAceito(participanteAceito);
        if (status != AcordoSessaoStatus.OPEN
                && status != AcordoSessaoStatus.PROPOSAL_PENDING
                && status != AcordoSessaoStatus.COUNTERPROPOSAL_PENDING) {
            throw new AcordoDomainException("Termo bloqueado pelo estado da sala: " + status);
        }
        if (propostaStatus == AcordoPropostaStatus.EXPIRADA || propostaExpirada) {
            throw new AcordoDomainException("Proposta expirada nao pode gerar termo.");
        }
        if (propostaStatus == AcordoPropostaStatus.REJEITADA || propostaStatus == AcordoPropostaStatus.SUBSTITUIDA) {
            throw new AcordoDomainException("Proposta sem vigencia nao pode gerar termo.");
        }
        if (criadaPorIa && !revisadaPorHumano) {
            throw new AcordoDomainException("Proposta criada por IA exige revisao humana.");
        }
    }

    public void requireAssinatura(AcordoSessaoStatus status, boolean expirada, boolean participanteAceito, boolean termoExistente, boolean papelPodeAssinar) {
        requireSalaAtiva(status, expirada);
        requireParticipanteAceito(participanteAceito);
        if (!termoExistente) {
            throw new AcordoDomainException("Assinatura exige termo gerado.");
        }
        if (!papelPodeAssinar) {
            throw new AcordoDomainException("Papel do participante nao pode assinar termo.");
        }
        if (status != AcordoSessaoStatus.AGREEMENT_DRAFTED && status != AcordoSessaoStatus.WAITING_SIGNATURES) {
            throw new AcordoDomainException("Assinatura bloqueada pelo estado da sala: " + status);
        }
    }

    public void requireEnvioHomologacao(AcordoSessaoStatus status, AcordoTermoStatus termoStatus) {
        if (status != AcordoSessaoStatus.SIGNED || termoStatus != AcordoTermoStatus.ASSINADO) {
            throw new AcordoDomainException("Envio para homologacao exige termo assinado.");
        }
    }

    public void requireHomologacao(AcordoSessaoStatus status, AcordoTermoStatus termoStatus) {
        if (status != AcordoSessaoStatus.SENT_TO_HOMOLOGATION || termoStatus != AcordoTermoStatus.ENVIADO_HOMOLOGACAO) {
            throw new AcordoDomainException("Homologacao exige envio previo de termo assinado.");
        }
    }

    public void requireRejeicao(AcordoSessaoStatus status, AcordoTermoStatus termoStatus, String motivo) {
        requireHomologacao(status, termoStatus);
        if (motivo == null || motivo.isBlank()) {
            throw new AcordoDomainException("Rejeicao judicial exige motivo.");
        }
    }

    public void requireInteracaoParticipanteAceito(boolean participanteAceito) {
        requireParticipanteAceito(participanteAceito);
    }

    private void requireSalaAtiva(AcordoSessaoStatus status, boolean expirada) {
        if (status == null || status.terminal()) {
            throw new AcordoDomainException("Sala sem interacao permitida no estado: " + status);
        }
        if (expirada) {
            throw new AcordoDomainException("Sala expirada nao aceita interacao.");
        }
    }

    private void requireParticipanteAceito(boolean participanteAceito) {
        if (!participanteAceito) {
            throw new AcordoDomainException("Participante precisa aceitar a sala antes de interagir.");
        }
    }
}
