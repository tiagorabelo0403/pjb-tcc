package com.tcc.pjb.backend.service.intelligence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

class AgreementChatGovernanceServiceTest {

    private final AgreementChatGovernanceService service = new AgreementChatGovernanceService();

    @Test
    void shouldBlockExternalTermChangeWhenProposalIsPendingJudgeDecision() {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        PropostaAcordo proposta = PropostaAcordo.builder().id(2L).status(StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ).build();
        Usuario advogado = new Usuario();
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);

        assertThrows(RegraNegocioException.class, () -> service.enforcePost(
                processo,
                proposta,
                advogado,
                "ACORDO_PROCESSUAL",
                "Nova proposta com alterar valor e parcelamento em seis vezes"
        ));
    }

    @Test
    void shouldAllowJudgeToHandleFrozenAgreementStage() {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        PropostaAcordo proposta = PropostaAcordo.builder().id(2L).status(StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ).build();
        Usuario juiz = new Usuario();
        juiz.setTipoUsuario(TipoUsuario.JUIZ);

        assertDoesNotThrow(() -> service.enforcePost(
                processo,
                proposta,
                juiz,
                "ACORDO_PROCESSUAL",
                "Devolver para revisão com ajuste de cronograma e cláusula penal."
        ));
    }
}
