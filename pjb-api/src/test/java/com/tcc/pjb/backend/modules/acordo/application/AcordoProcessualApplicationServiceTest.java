package com.tcc.pjb.backend.modules.acordo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.modules.acordo.api.AcordoAuditEntry;
import com.tcc.pjb.backend.modules.acordo.api.AuditoriaAcordoPort;
import com.tcc.pjb.backend.modules.acordo.api.MovimentacaoAcordoPort;
import com.tcc.pjb.backend.modules.acordo.api.ProcessoAcordoContexto;
import com.tcc.pjb.backend.modules.acordo.api.ProcessoAcordoPort;
import com.tcc.pjb.backend.modules.acordo.api.UsuarioAcordoPort;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoAuditoriaEvento;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoConfidencialidadeNivel;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoDomainException;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemTipo;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemVisibilidade;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPapelParticipante;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoParticipanteStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoSessaoStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoTermoStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoTipoSala;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AcordoProcessualApplicationServiceTest {

    @Test
    void naoAbreSalaForaDaJanelaProcessual() {
        Fixture fx = Fixture.create();
        fx.contextos.put(1L, contexto(false, false));
        fx.allow(1L, 10L);

        assertThatThrownBy(() -> fx.service.abrirSala(openCommand(1L, 10L)))
                .isInstanceOf(AcordoApplicationException.class)
                .hasMessageContaining("fora da janela");
    }

    @Test
    void abreSalaEmMomentoPermitidoAntesDaContestacao() {
        Fixture fx = Fixture.create();
        fx.allow(1L, 10L);

        AcordoSessaoSnapshot sala = fx.service.abrirSala(openCommand(1L, 10L));

        assertThat(sala.id()).isNotNull();
        assertThat(sala.tipoSala()).isEqualTo(AcordoTipoSala.CONCILIACAO);
        assertThat(sala.status()).isEqualTo(AcordoSessaoStatus.WAITING_PARTICIPANTS);
        assertThat(fx.auditEvents()).contains(AcordoAuditoriaEvento.ABERTURA);
    }

    @Test
    void processoSigilosoCriaSalaSigilosa() {
        Fixture fx = Fixture.create();
        fx.contextos.put(1L, contexto(true, true));
        fx.allow(1L, 10L);

        AcordoSessaoSnapshot sala = fx.service.abrirSala(openCommand(1L, 10L));

        assertThat(sala.segredoJustica()).isTrue();
        assertThat(sala.confidencialidadeNivel()).isEqualTo(AcordoConfidencialidadeNivel.SEGREDO_JUSTICA);
    }

    @Test
    void participantePrecisaAceitarAntesDeInteragir() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openWaitingWithInvite();

        assertThatThrownBy(() -> fx.service.registrarMensagem(new AcordoProcessualApplicationService.RegistrarMensagemCommand(
                sala.id(), 20L, AcordoMensagemTipo.TEXTO, "mensagem pendente", false, AcordoMensagemVisibilidade.PARTICIPANTES, meta())))
                .isInstanceOf(AcordoDomainException.class)
                .hasMessageContaining("precisa aceitar");
    }

    @Test
    void participanteNaoAutorizadoNaoAcessaSala() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();
        fx.users.add(30L);

        assertThatThrownBy(() -> fx.service.registrarMensagem(new AcordoProcessualApplicationService.RegistrarMensagemCommand(
                sala.id(), 30L, AcordoMensagemTipo.TEXTO, "tentativa", false, AcordoMensagemVisibilidade.PARTICIPANTES, meta())))
                .isInstanceOf(AcordoApplicationException.class)
                .hasMessageContaining("nao participa");
    }

    @Test
    void mensagemNaoCriaProposta() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();

        fx.service.registrarMensagem(new AcordoProcessualApplicationService.RegistrarMensagemCommand(
                sala.id(), 10L, AcordoMensagemTipo.TEXTO, "vamos avaliar composicao", false, AcordoMensagemVisibilidade.PARTICIPANTES, meta()));

        assertThat(fx.store.propostas).isEmpty();
    }

    @Test
    void propostaFormalExigeValidade() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();

        assertThatThrownBy(() -> fx.service.registrarProposta(new AcordoProcessualApplicationService.RegistrarPropostaCommand(
                sala.id(), 10L, BigDecimal.TEN, "{\"parcelas\":1}", null, false, meta())))
                .isInstanceOf(AcordoDomainException.class)
                .hasMessageContaining("validade futura");
    }

    @Test
    void propostaExpiradaNaoPodeGerarTermo() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();
        AcordoPropostaSnapshot proposta = fx.service.registrarProposta(proposalCommand(sala.id(), 10L, fx.now.plusSeconds(3600), false));
        fx.clock.now = fx.now.plusSeconds(7200);

        assertThatThrownBy(() -> fx.service.gerarMinutaTermo(new AcordoProcessualApplicationService.GerarMinutaTermoCommand(
                proposta.id(), 10L, "Termo final do acordo", meta())))
                .isInstanceOf(AcordoDomainException.class)
                .hasMessageContaining("expirada");
    }

    @Test
    void propostaCriadaPorIaExigeRevisaoHumanaAntesDoTermo() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();
        AcordoPropostaSnapshot proposta = fx.service.registrarProposta(proposalCommand(sala.id(), 10L, fx.now.plusSeconds(3600), true));

        assertThatThrownBy(() -> fx.service.gerarMinutaTermo(new AcordoProcessualApplicationService.GerarMinutaTermoCommand(
                proposta.id(), 10L, "Termo gerado sem revisao", meta())))
                .isInstanceOf(AcordoDomainException.class)
                .hasMessageContaining("revisao humana");

        fx.service.marcarRevisaoHumana(new AcordoProcessualApplicationService.MarcarRevisaoHumanaCommand(proposta.id(), 10L, meta()));
        AcordoTermoSnapshot termo = fx.service.gerarMinutaTermo(new AcordoProcessualApplicationService.GerarMinutaTermoCommand(
                proposta.id(), 10L, "Termo revisado por humano", meta()));
        assertThat(termo.status()).isEqualTo(AcordoTermoStatus.MINUTA);
    }

    @Test
    void termoNaoVaiParaHomologacaoSemAssinatura() {
        Fixture fx = Fixture.create();
        AcordoTermoSnapshot termo = fx.gerarTermoFormal();

        assertThatThrownBy(() -> fx.service.enviarParaHomologacao(new AcordoProcessualApplicationService.EnviarHomologacaoCommand(
                termo.id(), 10L, meta())))
                .isInstanceOf(AcordoDomainException.class)
                .hasMessageContaining("assinado");
    }

    @Test
    void termoAssinadoVaiParaHomologacao() {
        Fixture fx = Fixture.create();
        AcordoTermoSnapshot termo = fx.gerarTermoFormal();

        fx.service.assinarTermo(new AcordoProcessualApplicationService.AssinarTermoCommand(termo.id(), 10L, "hash-assinatura-parte-0001", meta()));
        AcordoTermoSnapshot enviado = fx.service.enviarParaHomologacao(new AcordoProcessualApplicationService.EnviarHomologacaoCommand(termo.id(), 10L, meta()));

        assertThat(enviado.status()).isEqualTo(AcordoTermoStatus.ENVIADO_HOMOLOGACAO);
        assertThat(fx.store.sessoes.get(termo.sessaoId()).status()).isEqualTo(AcordoSessaoStatus.SENT_TO_HOMOLOGATION);
    }

    @Test
    void homologacaoMudaStatusDaSalaERegistraMovimentacao() {
        Fixture fx = Fixture.create();
        AcordoTermoSnapshot termo = fx.termoEnviadoParaHomologacao();

        AcordoSessaoSnapshot homologada = fx.service.homologar(new AcordoProcessualApplicationService.HomologarCommand(
                termo.sessaoId(), 99L, "Sentenca homologatoria de acordo", meta()));

        assertThat(homologada.status()).isEqualTo(AcordoSessaoStatus.HOMOLOGATED);
        assertThat(homologada.homologadoPorId()).isEqualTo(99L);
        assertThat(fx.movimentos).hasSize(1);
        assertThat(fx.movimentos.get(0).tipo()).isEqualTo("HOMOLOGACAO");
    }

    @Test
    void rejeicaoRegistraMotivoNaAuditoria() {
        Fixture fx = Fixture.create();
        AcordoTermoSnapshot termo = fx.termoEnviadoParaHomologacao();

        fx.service.rejeitarHomologacao(new AcordoProcessualApplicationService.RejeitarHomologacaoCommand(
                termo.sessaoId(), 99L, "Clausula penal incompatvel com o titulo.", meta()));

        assertThat(fx.store.sessoes.get(termo.sessaoId()).status()).isEqualTo(AcordoSessaoStatus.REJECTED_BY_JUDGE);
        assertThat(fx.audits.stream().filter(audit -> audit.evento() == AcordoAuditoriaEvento.REJEICAO).toList())
                .singleElement()
                .satisfies(audit -> assertThat(audit.detalhes()).containsKey("motivo"));
    }

    @Test
    void salaExpiradaNaoAceitaMensagem() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();
        fx.clock.now = fx.now.plusSeconds(31 * 24 * 3600);

        assertThatThrownBy(() -> fx.service.registrarMensagem(new AcordoProcessualApplicationService.RegistrarMensagemCommand(
                sala.id(), 10L, AcordoMensagemTipo.TEXTO, "mensagem tardia", false, AcordoMensagemVisibilidade.PARTICIPANTES, meta())))
                .isInstanceOf(AcordoDomainException.class)
                .hasMessageContaining("expirada");
    }

    @Test
    void mensagemConfidencialNaoGeraMovimentacaoPublica() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();

        fx.service.registrarMensagem(new AcordoProcessualApplicationService.RegistrarMensagemCommand(
                sala.id(), 10L, AcordoMensagemTipo.TEXTO, "conteudo reservado", true, AcordoMensagemVisibilidade.CONFIDENCIAL, meta()));

        assertThat(fx.movimentos).isEmpty();
    }

    @Test
    void encerramentoSemAcordoRegistraAuditoriaEMovimentacao() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();

        AcordoSessaoSnapshot encerrada = fx.service.encerrarSemAcordo(new AcordoProcessualApplicationService.EncerrarSemAcordoCommand(
                sala.id(), 10L, "Partes sem convergencia material.", meta()));

        assertThat(encerrada.status()).isEqualTo(AcordoSessaoStatus.CLOSED);
        assertThat(fx.auditEvents()).contains(AcordoAuditoriaEvento.ENCERRAMENTO);
        assertThat(fx.movimentos).hasSize(1);
        assertThat(fx.movimentos.get(0).tipo()).isEqualTo("ENCERRAMENTO");
    }

    @Test
    void todaAcaoSensivelGeraAuditoria() {
        Fixture fx = Fixture.create();
        AcordoTermoSnapshot termo = fx.termoEnviadoParaHomologacao();
        fx.service.homologar(new AcordoProcessualApplicationService.HomologarCommand(termo.sessaoId(), 99L, "Homologado", meta()));

        assertThat(fx.auditEvents()).contains(
                AcordoAuditoriaEvento.ABERTURA,
                AcordoAuditoriaEvento.CONVITE,
                AcordoAuditoriaEvento.ACEITE,
                AcordoAuditoriaEvento.PROPOSTA,
                AcordoAuditoriaEvento.GERACAO_TERMO,
                AcordoAuditoriaEvento.ASSINATURA,
                AcordoAuditoriaEvento.ENVIO_HOMOLOGACAO,
                AcordoAuditoriaEvento.HOMOLOGACAO
        );
    }

    @Test
    void expirarSalasVencidasMarcaStatusEAudita() {
        Fixture fx = Fixture.create();
        AcordoSessaoSnapshot sala = fx.openReadyRoom();
        fx.clock.now = fx.now.plusSeconds(31 * 24 * 3600);

        int expiradas = fx.service.expirarSalasVencidas(10);

        assertThat(expiradas).isEqualTo(1);
        assertThat(fx.store.sessoes.get(sala.id()).status()).isEqualTo(AcordoSessaoStatus.EXPIRED);
        assertThat(fx.auditEvents()).contains(AcordoAuditoriaEvento.EXPIRACAO);
    }

    private static AcordoProcessualApplicationService.AbrirSalaCommand openCommand(Long processoId, Long usuarioId) {
        return new AcordoProcessualApplicationService.AbrirSalaCommand(
                processoId,
                usuarioId,
                AcordoPapelParticipante.PARTE,
                Instant.parse("2026-06-01T00:00:00Z"),
                "Requerimento de composicao processual controlada",
                false,
                false,
                false,
                false,
                false,
                false,
                meta()
        );
    }

    private static AcordoProcessualApplicationService.RegistrarPropostaCommand proposalCommand(Long sessaoId, Long autorId, Instant validade, boolean ia) {
        return new AcordoProcessualApplicationService.RegistrarPropostaCommand(
                sessaoId,
                autorId,
                BigDecimal.valueOf(1200),
                "{\"valor\":1200,\"parcelas\":2}",
                validade,
                ia,
                meta()
        );
    }

    private static AcordoOperationMetadata meta() {
        return AcordoOperationMetadata.empty();
    }

    private static ProcessoAcordoContexto contexto(boolean segredo, boolean permitido) {
        return new ProcessoAcordoContexto(
                1L,
                permitido ? "CITACAO" : "JULGAMENTO",
                segredo,
                permitido,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                80,
                permitido ? "antes da contestacao" : "sentenca iminente"
        );
    }

    private static final class Fixture {
        final Instant now = Instant.parse("2026-05-19T12:00:00Z");
        final MutableClock clock = new MutableClock(now);
        final InMemoryStore store = new InMemoryStore();
        final Map<Long, ProcessoAcordoContexto> contextos = new HashMap<>();
        final Set<Long> users = new HashSet<>();
        final Set<Long> judges = new HashSet<>();
        final Set<String> allowed = new HashSet<>();
        final List<AcordoAuditEntry> audits = new ArrayList<>();
        final List<Movimento> movimentos = new ArrayList<>();
        final AcordoProcessualApplicationService service;

        private Fixture() {
            contextos.put(1L, contexto(false, true));
            users.addAll(List.of(10L, 20L, 99L));
            judges.add(99L);
            allow(1L, 10L);
            allow(1L, 20L);
            ProcessoAcordoPort processoPort = new FakeProcessoPort(contextos, movimentos);
            UsuarioAcordoPort usuarioPort = new FakeUsuarioPort(users, judges, allowed);
            AuditoriaAcordoPort auditoriaPort = audits::add;
            MovimentacaoAcordoPort movimentacaoPort = new FakeMovimentacaoPort(movimentos);
            service = new AcordoProcessualApplicationService(store, processoPort, usuarioPort, auditoriaPort, movimentacaoPort, clock);
        }

        static Fixture create() {
            return new Fixture();
        }

        void allow(Long processoId, Long usuarioId) {
            allowed.add(processoId + ":" + usuarioId);
        }

        AcordoSessaoSnapshot openWaitingWithInvite() {
            AcordoSessaoSnapshot sala = service.abrirSala(openCommand(1L, 10L));
            service.convidarParticipante(new AcordoProcessualApplicationService.ConvidarParticipanteCommand(
                    sala.id(), 10L, 20L, AcordoPapelParticipante.PARTE, meta()));
            return sala;
        }

        AcordoSessaoSnapshot openReadyRoom() {
            AcordoSessaoSnapshot sala = openWaitingWithInvite();
            service.aceitarParticipacao(new AcordoProcessualApplicationService.ParticipacaoCommand(sala.id(), 20L, meta()));
            return store.sessoes.get(sala.id());
        }

        AcordoTermoSnapshot gerarTermoFormal() {
            AcordoSessaoSnapshot sala = openReadyRoom();
            AcordoPropostaSnapshot proposta = service.registrarProposta(proposalCommand(sala.id(), 10L, now.plusSeconds(3600), false));
            return service.gerarMinutaTermo(new AcordoProcessualApplicationService.GerarMinutaTermoCommand(
                    proposta.id(), 10L, "Termo formal de acordo processual", meta()));
        }

        AcordoTermoSnapshot termoEnviadoParaHomologacao() {
            AcordoTermoSnapshot termo = gerarTermoFormal();
            service.assinarTermo(new AcordoProcessualApplicationService.AssinarTermoCommand(termo.id(), 10L, "hash-assinatura-parte-0001", meta()));
            return service.enviarParaHomologacao(new AcordoProcessualApplicationService.EnviarHomologacaoCommand(termo.id(), 10L, meta()));
        }

        List<AcordoAuditoriaEvento> auditEvents() {
            return audits.stream().map(AcordoAuditEntry::evento).toList();
        }
    }

    private static final class InMemoryStore implements AcordoProcessualStorePort {
        long seqSessao;
        long seqParticipante;
        long seqMensagem;
        long seqProposta;
        long seqTermo;
        final Map<Long, AcordoSessaoSnapshot> sessoes = new HashMap<>();
        final Map<Long, AcordoParticipanteSnapshot> participantes = new HashMap<>();
        final Map<Long, AcordoMensagemSnapshot> mensagens = new HashMap<>();
        final Map<Long, AcordoPropostaSnapshot> propostas = new HashMap<>();
        final Map<Long, AcordoTermoSnapshot> termos = new HashMap<>();

        @Override
        public AcordoSessaoSnapshot saveSessao(AcordoSessaoSnapshot sessao) {
            Long id = sessao.id() == null ? ++seqSessao : sessao.id();
            AcordoSessaoSnapshot saved = new AcordoSessaoSnapshot(id, sessao.processoId(), sessao.tipoSala(), sessao.status(), sessao.abertaPorId(),
                    sessao.abertaEm(), sessao.expiraEm(), sessao.motivoAbertura(), sessao.segredoJustica(), sessao.confidencialidadeNivel(),
                    sessao.cejuscReferenciado(), sessao.homologadoEm(), sessao.homologadoPorId(), sessao.createdAt());
            sessoes.put(id, saved);
            return saved;
        }

        @Override
        public Optional<AcordoSessaoSnapshot> findSessao(Long sessaoId) {
            return Optional.ofNullable(sessoes.get(sessaoId));
        }

        @Override
        public Optional<AcordoSessaoSnapshot> findSessaoForUpdate(Long sessaoId) {
            return findSessao(sessaoId);
        }

        @Override
        public AcordoParticipanteSnapshot saveParticipante(AcordoParticipanteSnapshot participante) {
            Long id = participante.id() == null ? ++seqParticipante : participante.id();
            AcordoParticipanteSnapshot saved = new AcordoParticipanteSnapshot(id, participante.sessaoId(), participante.usuarioId(), participante.papel(),
                    participante.status(), participante.aceitouEm(), participante.recusouEm(), participante.createdAt());
            participantes.put(id, saved);
            return saved;
        }

        @Override
        public Optional<AcordoParticipanteSnapshot> findParticipante(Long sessaoId, Long usuarioId) {
            return participantes.values().stream()
                    .filter(p -> p.sessaoId().equals(sessaoId) && p.usuarioId().equals(usuarioId))
                    .findFirst();
        }

        @Override
        public List<AcordoParticipanteSnapshot> findParticipantes(Long sessaoId) {
            return participantes.values().stream().filter(p -> p.sessaoId().equals(sessaoId)).toList();
        }

        @Override
        public long countParticipantesAceitos(Long sessaoId) {
            return participantes.values().stream()
                    .filter(p -> p.sessaoId().equals(sessaoId) && p.status() == AcordoParticipanteStatus.ACEITO)
                    .count();
        }

        @Override
        public AcordoMensagemSnapshot saveMensagem(AcordoMensagemSnapshot mensagem) {
            Long id = mensagem.id() == null ? ++seqMensagem : mensagem.id();
            AcordoMensagemSnapshot saved = new AcordoMensagemSnapshot(id, mensagem.sessaoId(), mensagem.autorId(), mensagem.tipo(),
                    mensagem.conteudo(), mensagem.confidencial(), mensagem.visibilidade(), mensagem.createdAt());
            mensagens.put(id, saved);
            return saved;
        }

        @Override
        public AcordoPropostaSnapshot saveProposta(AcordoPropostaSnapshot proposta) {
            Long id = proposta.id() == null ? ++seqProposta : proposta.id();
            AcordoPropostaSnapshot saved = new AcordoPropostaSnapshot(id, proposta.sessaoId(), proposta.autorId(), proposta.tipo(), proposta.valor(),
                    proposta.termosJson(), proposta.validadeAte(), proposta.status(), proposta.criadaPorIa(), proposta.revisadaPorHumano(),
                    proposta.revisadaPorId(), proposta.revisadaEm(), proposta.createdAt());
            propostas.put(id, saved);
            return saved;
        }

        @Override
        public Optional<AcordoPropostaSnapshot> findProposta(Long propostaId) {
            return Optional.ofNullable(propostas.get(propostaId));
        }

        @Override
        public Optional<AcordoPropostaSnapshot> findPropostaForUpdate(Long propostaId) {
            return findProposta(propostaId);
        }

        @Override
        public AcordoTermoSnapshot saveTermo(AcordoTermoSnapshot termo) {
            Long id = termo.id() == null ? ++seqTermo : termo.id();
            AcordoTermoSnapshot saved = new AcordoTermoSnapshot(id, termo.sessaoId(), termo.propostaId(), termo.conteudoTermo(),
                    termo.hashTermo(), termo.status(), termo.createdAt());
            termos.put(id, saved);
            return saved;
        }

        @Override
        public Optional<AcordoTermoSnapshot> findTermoForUpdate(Long termoId) {
            return Optional.ofNullable(termos.get(termoId));
        }

        @Override
        public Optional<AcordoTermoSnapshot> findTermoBySessao(Long sessaoId) {
            return termos.values().stream()
                    .filter(t -> t.sessaoId().equals(sessaoId))
                    .max(Comparator.comparing(AcordoTermoSnapshot::id));
        }

        @Override
        public Optional<AcordoTermoSnapshot> findTermoByProposta(Long propostaId) {
            return termos.values().stream().filter(t -> t.propostaId().equals(propostaId)).findFirst();
        }

        @Override
        public List<AcordoSessaoSnapshot> findSessoesExpiradas(Instant now, int limit) {
            return sessoes.values().stream()
                    .filter(s -> s.expiradaEm(now) && !s.status().terminal())
                    .sorted(Comparator.comparing(AcordoSessaoSnapshot::expiraEm))
                    .limit(limit)
                    .toList();
        }
    }

    private static final class FakeProcessoPort implements ProcessoAcordoPort {
        private final Map<Long, ProcessoAcordoContexto> contextos;
        private final List<Movimento> movimentos;

        private FakeProcessoPort(Map<Long, ProcessoAcordoContexto> contextos, List<Movimento> movimentos) {
            this.contextos = contextos;
            this.movimentos = movimentos;
        }

        @Override
        public boolean existeProcesso(Long processoId) {
            return contextos.containsKey(processoId);
        }

        @Override
        public ProcessoAcordoContexto obterContextoProcessual(Long processoId) {
            return contextos.get(processoId);
        }

        @Override
        public boolean processoEstaEmSegredo(Long processoId) {
            return Optional.ofNullable(contextos.get(processoId)).map(ProcessoAcordoContexto::segredoJustica).orElse(false);
        }

        @Override
        public void registrarMovimentacaoAcordo(Long processoId, String tipo, String descricao) {
            movimentos.add(new Movimento("PROCESSO_PORT", processoId, tipo, descricao));
        }
    }

    private static final class FakeUsuarioPort implements UsuarioAcordoPort {
        private final Set<Long> users;
        private final Set<Long> judges;
        private final Set<String> allowed;

        private FakeUsuarioPort(Set<Long> users, Set<Long> judges, Set<String> allowed) {
            this.users = users;
            this.judges = judges;
            this.allowed = allowed;
        }

        @Override
        public boolean existeUsuario(Long usuarioId) {
            return users.contains(usuarioId);
        }

        @Override
        public boolean usuarioPodeParticipar(Long processoId, Long usuarioId) {
            return allowed.contains(processoId + ":" + usuarioId);
        }

        @Override
        public boolean usuarioPodeHomologar(Long usuarioId) {
            return judges.contains(usuarioId);
        }
    }

    private static final class FakeMovimentacaoPort implements MovimentacaoAcordoPort {
        private final List<Movimento> movimentos;

        private FakeMovimentacaoPort(List<Movimento> movimentos) {
            this.movimentos = movimentos;
        }

        @Override
        public void registrarHomologacao(Long processoId, Long magistradoId, String descricao) {
            movimentos.add(new Movimento("HOMOLOGACAO", processoId, String.valueOf(magistradoId), descricao));
        }

        @Override
        public void registrarEncerramentoSemAcordo(Long processoId, Long usuarioId, String descricao) {
            movimentos.add(new Movimento("ENCERRAMENTO", processoId, String.valueOf(usuarioId), descricao));
        }
    }

    private record Movimento(String tipo, Long processoId, String ator, String descricao) {
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
