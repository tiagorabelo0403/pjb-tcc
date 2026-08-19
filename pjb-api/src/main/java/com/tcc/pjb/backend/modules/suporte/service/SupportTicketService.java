package com.tcc.pjb.backend.modules.suporte.service;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.suporte.dto.AbrirChamadoRequest;
import com.tcc.pjb.backend.modules.suporte.dto.ResolverChamadoRequest;
import com.tcc.pjb.backend.modules.suporte.dto.SupportTicketResponse;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicket;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import com.tcc.pjb.backend.modules.suporte.repository.SupportTicketRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.notification.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketService {

    private static final List<SupportTicketStatus> STATUS_ABERTOS =
            List.of(SupportTicketStatus.ABERTO, SupportTicketStatus.EM_ATENDIMENTO);

    private final SupportTicketRepository repository;
    private final NotificationService notificationService;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SupportTicketService(SupportTicketRepository repository,
                                 NotificationService notificationService,
                                 UsuarioRepository usuarioRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Transactional
    public SupportTicketResponse abrir(Usuario usuario, AbrirChamadoRequest request) {
        if (request.categoria() == SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA) {
            if (request.viagemUfOuPaisDestino() == null || request.viagemDataInicio() == null || request.viagemDataFim() == null) {
                throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                        "Exceção de viagem exige destino e janela de datas completa.");
            }
            if (request.viagemDataFim().isBefore(request.viagemDataInicio())) {
                throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                        "Data final da viagem não pode ser anterior à data inicial.");
            }
        }

        SupportTicket ticket = SupportTicket.builder()
                .abertoPorId(usuario.getId())
                .abertoPorNome(usuario.getNome())
                .abertoPorTipoUsuario(usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null)
                .categoria(request.categoria())
                .assunto(request.assunto())
                .descricao(request.descricao())
                .status(SupportTicketStatus.ABERTO)
                .viagemUfOuPaisDestino(request.viagemUfOuPaisDestino())
                .viagemDataInicio(request.viagemDataInicio())
                .viagemDataFim(request.viagemDataFim())
                .viagemJustificativa(request.viagemJustificativa())
                .criadoEm(Instant.now())
                .build();

        return SupportTicketResponse.de(repository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> meusChamados(Long usuarioId) {
        return repository.findByAbertoPorId(usuarioId).stream().map(SupportTicketResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> fila(SupportTicketStatus status) {
        List<SupportTicketStatus> statuses = status != null ? List.of(status) : STATUS_ABERTOS;
        return repository.findFila(statuses).stream().map(SupportTicketResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public long countAbertosPorUsuario(Long usuarioId) {
        return repository.countByAbertoPorIdAndStatusIn(usuarioId, STATUS_ABERTOS);
    }

    @Transactional
    public SupportTicketResponse assumir(Long ticketId, Usuario atendente) {
        SupportTicket ticket = buscar(ticketId);
        if (ticket.getStatus() != SupportTicketStatus.ABERTO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Chamado não está aberto.");
        }
        ticket.setStatus(SupportTicketStatus.EM_ATENDIMENTO);
        ticket.setAtendidoPorId(atendente.getId());
        ticket.setAtendidoPorNome(atendente.getNome());
        ticket.setAtendidoEm(Instant.now());
        SupportTicket salvo = repository.save(ticket);
        notificarAbertoPor(salvo, "Seu chamado foi assumido",
                "Um atendente do suporte está cuidando do seu chamado #" + salvo.getId() + ".");
        return SupportTicketResponse.de(salvo);
    }

    @Transactional
    public SupportTicketResponse resolver(Long ticketId, Usuario atendente, ResolverChamadoRequest request) {
        SupportTicket ticket = buscar(ticketId);
        if (ticket.getStatus() != SupportTicketStatus.EM_ATENDIMENTO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                    "Chamado precisa estar em atendimento para ser resolvido.");
        }
        ticket.setStatus(SupportTicketStatus.RESOLVIDO);
        ticket.setResposta(request.resposta());
        ticket.setResolvidoEm(Instant.now());
        SupportTicket salvo = repository.save(ticket);

        notificarAbertoPor(salvo, "Seu chamado foi resolvido", request.resposta());

        eventPublisher.publishEvent(new SupportTicketResolvedEvent(
                salvo.getId(), salvo.getCategoria(), request.aprovado(), salvo.getAbertoPorId(),
                salvo.getViagemUfOuPaisDestino(), salvo.getViagemDataInicio(), salvo.getViagemDataFim()));

        return SupportTicketResponse.de(salvo);
    }

    @Transactional
    public SupportTicketResponse fechar(Long ticketId, Usuario atendente) {
        SupportTicket ticket = buscar(ticketId);
        if (ticket.getStatus() == SupportTicketStatus.FECHADO || ticket.getStatus() == SupportTicketStatus.CANCELADO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Chamado já está encerrado.");
        }
        ticket.setStatus(SupportTicketStatus.FECHADO);
        SupportTicket salvo = repository.save(ticket);
        notificarAbertoPor(salvo, "Seu chamado foi encerrado",
                "O chamado #" + salvo.getId() + " foi encerrado pelo suporte.");
        return SupportTicketResponse.de(salvo);
    }

    @Transactional
    public SupportTicketResponse cancelar(Long ticketId, Usuario solicitante) {
        SupportTicket ticket = buscar(ticketId);
        if (!ticket.getAbertoPorId().equals(solicitante.getId())) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                    "Só quem abriu o chamado pode cancelá-lo.");
        }
        if (ticket.getStatus() != SupportTicketStatus.ABERTO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                    "Só é possível cancelar um chamado ainda aberto.");
        }
        ticket.setStatus(SupportTicketStatus.CANCELADO);
        return SupportTicketResponse.de(repository.save(ticket));
    }

    private SupportTicket buscar(Long ticketId) {
        return repository.findById(ticketId)
                .orElseThrow(() -> new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                        "Chamado não encontrado: " + ticketId));
    }

    private void notificarAbertoPor(SupportTicket ticket, String titulo, String mensagem) {
        usuarioRepository.findById(ticket.getAbertoPorId())
                .ifPresent(usuario -> notificationService.notifyUser(usuario, null, titulo, mensagem,
                        "/suporte/chamados/" + ticket.getId()));
    }
}
