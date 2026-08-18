package com.tcc.pjb.backend.modules.acordo.application;

import com.tcc.pjb.backend.modules.acordo.api.AcordoProcessualChatContext;
import com.tcc.pjb.backend.modules.acordo.api.AcordoProcessualChatMessageResult;
import com.tcc.pjb.backend.modules.acordo.api.UsuarioAcordoPort;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemTipo;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemVisibilidade;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcordoProcessualChatBridgeService {

    private final AcordoProcessualStorePort store;
    private final AcordoProcessualApplicationService applicationService;
    private final UsuarioAcordoPort usuarioPort;
    private final Clock clock;

    public AcordoProcessualChatBridgeService(AcordoProcessualStorePort store,
                                             AcordoProcessualApplicationService applicationService,
                                             UsuarioAcordoPort usuarioPort,
                                             Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.applicationService = Objects.requireNonNull(applicationService);
        this.usuarioPort = Objects.requireNonNull(usuarioPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional(readOnly = true)
    public AcordoProcessualChatContext obterContexto(Long processoId) {
        Long id = requireId(processoId, "processoId");
        Instant now = Instant.now(clock);
        return store.findSessaoAtivaByProcesso(id, now)
                .map(sessao -> new AcordoProcessualChatContext(
                        id,
                        sessao.id(),
                        sessao.status().name(),
                        sessao.tipoSala().name(),
                        sessao.confidencialidadeNivel().name(),
                        sessao.segredoJustica(),
                        !sessao.status().terminal() && !sessao.expiradaEm(now),
                        sessao.expiraEm(),
                        Math.toIntExact(Math.min(Integer.MAX_VALUE, store.countParticipantesAceitos(sessao.id())))
                ))
                .orElseGet(() -> AcordoProcessualChatContext.semSala(id));
    }

    @Transactional
    public AcordoProcessualChatMessageResult registrarMensagemDoChat(AcordoProcessualChatMessageCommand command) {
        Objects.requireNonNull(command, "command");
        Long processoId = requireId(command.processoId(), "processoId");
        Long autorId = requireId(command.autorId(), "autorId");
        String conteudo = requireText(command.conteudo(), "conteudo", 8000);
        Instant now = Instant.now(clock);
        var sessaoOptional = store.findSessaoAtivaByProcesso(processoId, now);
        if (sessaoOptional.isEmpty()) {
            if (command.exigirSala()) {
                throw new AcordoApplicationException("Canal de acordo exige sala de acordo processual ativa.");
            }
            return AcordoProcessualChatMessageResult.ignorada("Processo sem sala de acordo ativa.");
        }
        AcordoSessaoSnapshot sessao = sessaoOptional.get();
        if (!usuarioPort.usuarioPodeParticipar(processoId, autorId)) {
            throw new AcordoApplicationException("Usuario nao autorizado a participar da sala de acordo.");
        }
        AcordoParticipanteSnapshot participante = store.findParticipante(sessao.id(), autorId)
                .orElseThrow(() -> new AcordoApplicationException("Usuario precisa ser convidado para a sala de acordo antes de usar o canal negocial."));
        if (!participante.aceito()) {
            throw new AcordoApplicationException("Usuario precisa aceitar a participacao na sala de acordo antes de enviar mensagem negocial.");
        }
        AcordoMensagemSnapshot mensagem = applicationService.registrarMensagem(new AcordoProcessualApplicationService.RegistrarMensagemCommand(
                sessao.id(),
                autorId,
                command.tipo() != null ? command.tipo() : AcordoMensagemTipo.TEXTO,
                conteudo,
                command.confidencial(),
                command.confidencial() ? AcordoMensagemVisibilidade.CONFIDENCIAL : AcordoMensagemVisibilidade.PARTICIPANTES,
                command.metadata() != null ? command.metadata() : AcordoOperationMetadata.empty()
        ));
        AcordoSessaoSnapshot atualizada = store.findSessao(sessao.id()).orElse(sessao);
        return new AcordoProcessualChatMessageResult(
                true,
                atualizada.id(),
                mensagem.id(),
                atualizada.status().name(),
                atualizada.confidencialidadeNivel().name(),
                "Mensagem registrada na sala de acordo processual."
        );
    }

    private Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new AcordoApplicationException(field + " invalido.");
        }
        return value;
    }

    private String requireText(String value, String field, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new AcordoApplicationException(field + " obrigatorio.");
        }
        if (normalized.length() > max) {
            throw new AcordoApplicationException(field + " excede tamanho maximo.");
        }
        return normalized;
    }

    public record AcordoProcessualChatMessageCommand(
            Long processoId,
            Long autorId,
            AcordoMensagemTipo tipo,
            String conteudo,
            boolean confidencial,
            boolean exigirSala,
            AcordoOperationMetadata metadata
    ) {
    }
}
