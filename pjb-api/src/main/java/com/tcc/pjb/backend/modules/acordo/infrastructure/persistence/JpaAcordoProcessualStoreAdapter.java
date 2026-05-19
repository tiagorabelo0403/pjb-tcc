package com.tcc.pjb.backend.modules.acordo.infrastructure.persistence;

import com.tcc.pjb.backend.modules.acordo.application.AcordoMensagemSnapshot;
import com.tcc.pjb.backend.modules.acordo.application.AcordoParticipanteSnapshot;
import com.tcc.pjb.backend.modules.acordo.application.AcordoProcessualStorePort;
import com.tcc.pjb.backend.modules.acordo.application.AcordoPropostaSnapshot;
import com.tcc.pjb.backend.modules.acordo.application.AcordoSessaoSnapshot;
import com.tcc.pjb.backend.modules.acordo.application.AcordoTermoSnapshot;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoParticipanteStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoSessaoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAcordoProcessualStoreAdapter implements AcordoProcessualStorePort {

    private final AcordoSessaoJpaRepository sessaoRepository;
    private final AcordoParticipanteJpaRepository participanteRepository;
    private final AcordoMensagemJpaRepository mensagemRepository;
    private final AcordoPropostaJpaRepository propostaRepository;
    private final AcordoTermoJpaRepository termoRepository;

    public JpaAcordoProcessualStoreAdapter(AcordoSessaoJpaRepository sessaoRepository,
                                           AcordoParticipanteJpaRepository participanteRepository,
                                           AcordoMensagemJpaRepository mensagemRepository,
                                           AcordoPropostaJpaRepository propostaRepository,
                                           AcordoTermoJpaRepository termoRepository) {
        this.sessaoRepository = sessaoRepository;
        this.participanteRepository = participanteRepository;
        this.mensagemRepository = mensagemRepository;
        this.propostaRepository = propostaRepository;
        this.termoRepository = termoRepository;
    }

    @Override
    public AcordoSessaoSnapshot saveSessao(AcordoSessaoSnapshot sessao) {
        AcordoSessaoEntity entity = sessao.id() == null
                ? new AcordoSessaoEntity()
                : sessaoRepository.findById(sessao.id()).orElseGet(AcordoSessaoEntity::new);
        entity.setId(sessao.id());
        entity.setProcessoId(sessao.processoId());
        entity.setTipoSala(sessao.tipoSala());
        entity.setStatus(sessao.status());
        entity.setAbertaPorId(sessao.abertaPorId());
        entity.setAbertaEm(sessao.abertaEm());
        entity.setExpiraEm(sessao.expiraEm());
        entity.setMotivoAbertura(sessao.motivoAbertura());
        entity.setSegredoJustica(sessao.segredoJustica());
        entity.setConfidencialidadeNivel(sessao.confidencialidadeNivel());
        entity.setCejuscReferenciado(sessao.cejuscReferenciado());
        entity.setHomologadoEm(sessao.homologadoEm());
        entity.setHomologadoPorId(sessao.homologadoPorId());
        entity.setCreatedAt(sessao.createdAt());
        return toSnapshot(sessaoRepository.save(entity));
    }

    @Override
    public Optional<AcordoSessaoSnapshot> findSessao(Long sessaoId) {
        return sessaoRepository.findById(sessaoId).map(this::toSnapshot);
    }

    @Override
    public Optional<AcordoSessaoSnapshot> findSessaoForUpdate(Long sessaoId) {
        return sessaoRepository.findByIdForUpdate(sessaoId).map(this::toSnapshot);
    }

    @Override
    public AcordoParticipanteSnapshot saveParticipante(AcordoParticipanteSnapshot participante) {
        AcordoParticipanteEntity entity = participante.id() == null
                ? new AcordoParticipanteEntity()
                : participanteRepository.findById(participante.id()).orElseGet(AcordoParticipanteEntity::new);
        entity.setId(participante.id());
        entity.setSessaoId(participante.sessaoId());
        entity.setUsuarioId(participante.usuarioId());
        entity.setPapel(participante.papel());
        entity.setStatus(participante.status());
        entity.setAceitouEm(participante.aceitouEm());
        entity.setRecusouEm(participante.recusouEm());
        entity.setCreatedAt(participante.createdAt());
        return toSnapshot(participanteRepository.save(entity));
    }

    @Override
    public Optional<AcordoParticipanteSnapshot> findParticipante(Long sessaoId, Long usuarioId) {
        return participanteRepository.findBySessaoIdAndUsuarioId(sessaoId, usuarioId).map(this::toSnapshot);
    }

    @Override
    public List<AcordoParticipanteSnapshot> findParticipantes(Long sessaoId) {
        return participanteRepository.findBySessaoIdOrderByIdAsc(sessaoId).stream().map(this::toSnapshot).toList();
    }

    @Override
    public long countParticipantesAceitos(Long sessaoId) {
        return participanteRepository.countBySessaoIdAndStatus(sessaoId, AcordoParticipanteStatus.ACEITO);
    }

    @Override
    public AcordoMensagemSnapshot saveMensagem(AcordoMensagemSnapshot mensagem) {
        AcordoMensagemEntity entity = new AcordoMensagemEntity();
        entity.setId(mensagem.id());
        entity.setSessaoId(mensagem.sessaoId());
        entity.setAutorId(mensagem.autorId());
        entity.setTipo(mensagem.tipo());
        entity.setConteudo(mensagem.conteudo());
        entity.setConfidencial(mensagem.confidencial());
        entity.setVisibilidade(mensagem.visibilidade());
        entity.setCreatedAt(mensagem.createdAt());
        return toSnapshot(mensagemRepository.save(entity));
    }

    @Override
    public AcordoPropostaSnapshot saveProposta(AcordoPropostaSnapshot proposta) {
        AcordoPropostaEntity entity = proposta.id() == null
                ? new AcordoPropostaEntity()
                : propostaRepository.findById(proposta.id()).orElseGet(AcordoPropostaEntity::new);
        entity.setId(proposta.id());
        entity.setSessaoId(proposta.sessaoId());
        entity.setAutorId(proposta.autorId());
        entity.setTipo(proposta.tipo());
        entity.setValor(proposta.valor());
        entity.setTermosJson(proposta.termosJson());
        entity.setValidadeAte(proposta.validadeAte());
        entity.setStatus(proposta.status());
        entity.setCriadaPorIa(proposta.criadaPorIa());
        entity.setRevisadaPorHumano(proposta.revisadaPorHumano());
        entity.setRevisadaPorId(proposta.revisadaPorId());
        entity.setRevisadaEm(proposta.revisadaEm());
        entity.setCreatedAt(proposta.createdAt());
        return toSnapshot(propostaRepository.save(entity));
    }

    @Override
    public Optional<AcordoPropostaSnapshot> findProposta(Long propostaId) {
        return propostaRepository.findById(propostaId).map(this::toSnapshot);
    }

    @Override
    public Optional<AcordoPropostaSnapshot> findPropostaForUpdate(Long propostaId) {
        return propostaRepository.findByIdForUpdate(propostaId).map(this::toSnapshot);
    }

    @Override
    public AcordoTermoSnapshot saveTermo(AcordoTermoSnapshot termo) {
        AcordoTermoEntity entity = termo.id() == null
                ? new AcordoTermoEntity()
                : termoRepository.findById(termo.id()).orElseGet(AcordoTermoEntity::new);
        entity.setId(termo.id());
        entity.setSessaoId(termo.sessaoId());
        entity.setPropostaId(termo.propostaId());
        entity.setConteudoTermo(termo.conteudoTermo());
        entity.setHashTermo(termo.hashTermo());
        entity.setStatus(termo.status());
        entity.setCreatedAt(termo.createdAt());
        return toSnapshot(termoRepository.save(entity));
    }

    @Override
    public Optional<AcordoTermoSnapshot> findTermoForUpdate(Long termoId) {
        return termoRepository.findByIdForUpdate(termoId).map(this::toSnapshot);
    }

    @Override
    public Optional<AcordoTermoSnapshot> findTermoBySessao(Long sessaoId) {
        return termoRepository.findFirstBySessaoIdOrderByIdDesc(sessaoId).map(this::toSnapshot);
    }

    @Override
    public Optional<AcordoTermoSnapshot> findTermoByProposta(Long propostaId) {
        return termoRepository.findByPropostaId(propostaId).map(this::toSnapshot);
    }

    @Override
    public List<AcordoSessaoSnapshot> findSessoesExpiradas(Instant now, int limit) {
        return sessaoRepository.findSessoesExpiradas(now, terminalStatuses(), PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(this::toSnapshot)
                .toList();
    }

    private List<AcordoSessaoStatus> terminalStatuses() {
        return List.of(
                AcordoSessaoStatus.HOMOLOGATED,
                AcordoSessaoStatus.REJECTED_BY_JUDGE,
                AcordoSessaoStatus.FAILED,
                AcordoSessaoStatus.EXPIRED,
                AcordoSessaoStatus.CLOSED,
                AcordoSessaoStatus.NOT_ELIGIBLE
        );
    }

    private AcordoSessaoSnapshot toSnapshot(AcordoSessaoEntity entity) {
        return new AcordoSessaoSnapshot(
                entity.getId(),
                entity.getProcessoId(),
                entity.getTipoSala(),
                entity.getStatus(),
                entity.getAbertaPorId(),
                entity.getAbertaEm(),
                entity.getExpiraEm(),
                entity.getMotivoAbertura(),
                entity.isSegredoJustica(),
                entity.getConfidencialidadeNivel(),
                entity.isCejuscReferenciado(),
                entity.getHomologadoEm(),
                entity.getHomologadoPorId(),
                entity.getCreatedAt()
        );
    }

    private AcordoParticipanteSnapshot toSnapshot(AcordoParticipanteEntity entity) {
        return new AcordoParticipanteSnapshot(
                entity.getId(),
                entity.getSessaoId(),
                entity.getUsuarioId(),
                entity.getPapel(),
                entity.getStatus(),
                entity.getAceitouEm(),
                entity.getRecusouEm(),
                entity.getCreatedAt()
        );
    }

    private AcordoMensagemSnapshot toSnapshot(AcordoMensagemEntity entity) {
        return new AcordoMensagemSnapshot(
                entity.getId(),
                entity.getSessaoId(),
                entity.getAutorId(),
                entity.getTipo(),
                entity.getConteudo(),
                entity.isConfidencial(),
                entity.getVisibilidade(),
                entity.getCreatedAt()
        );
    }

    private AcordoPropostaSnapshot toSnapshot(AcordoPropostaEntity entity) {
        return new AcordoPropostaSnapshot(
                entity.getId(),
                entity.getSessaoId(),
                entity.getAutorId(),
                entity.getTipo(),
                entity.getValor(),
                entity.getTermosJson(),
                entity.getValidadeAte(),
                entity.getStatus(),
                entity.isCriadaPorIa(),
                entity.isRevisadaPorHumano(),
                entity.getRevisadaPorId(),
                entity.getRevisadaEm(),
                entity.getCreatedAt()
        );
    }

    private AcordoTermoSnapshot toSnapshot(AcordoTermoEntity entity) {
        return new AcordoTermoSnapshot(
                entity.getId(),
                entity.getSessaoId(),
                entity.getPropostaId(),
                entity.getConteudoTermo(),
                entity.getHashTermo(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
