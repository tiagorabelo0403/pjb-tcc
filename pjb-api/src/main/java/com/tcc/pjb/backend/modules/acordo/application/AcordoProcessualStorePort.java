package com.tcc.pjb.backend.modules.acordo.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AcordoProcessualStorePort {

    AcordoSessaoSnapshot saveSessao(AcordoSessaoSnapshot sessao);

    Optional<AcordoSessaoSnapshot> findSessao(Long sessaoId);

    Optional<AcordoSessaoSnapshot> findSessaoForUpdate(Long sessaoId);

    Optional<AcordoSessaoSnapshot> findSessaoAtivaByProcesso(Long processoId, Instant now);

    AcordoParticipanteSnapshot saveParticipante(AcordoParticipanteSnapshot participante);

    Optional<AcordoParticipanteSnapshot> findParticipante(Long sessaoId, Long usuarioId);

    List<AcordoParticipanteSnapshot> findParticipantes(Long sessaoId);

    long countParticipantesAceitos(Long sessaoId);

    AcordoMensagemSnapshot saveMensagem(AcordoMensagemSnapshot mensagem);

    AcordoPropostaSnapshot saveProposta(AcordoPropostaSnapshot proposta);

    Optional<AcordoPropostaSnapshot> findProposta(Long propostaId);

    Optional<AcordoPropostaSnapshot> findPropostaForUpdate(Long propostaId);

    AcordoTermoSnapshot saveTermo(AcordoTermoSnapshot termo);

    Optional<AcordoTermoSnapshot> findTermoForUpdate(Long termoId);

    Optional<AcordoTermoSnapshot> findTermoBySessao(Long sessaoId);

    Optional<AcordoTermoSnapshot> findTermoByProposta(Long propostaId);

    List<AcordoSessaoSnapshot> findSessoesExpiradas(Instant now, int limit);
}
