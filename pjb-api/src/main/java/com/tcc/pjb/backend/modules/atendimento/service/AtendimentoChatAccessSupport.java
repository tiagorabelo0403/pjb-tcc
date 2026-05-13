package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoCreateThreadRequest;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
class AtendimentoChatAccessSupport {

    private final PjbAuthorizationService authorizationService;
    private final ProcessoRepository processoRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    AtendimentoChatAccessSupport(PjbAuthorizationService authorizationService,
                                 ProcessoRepository processoRepository,
                                 LaianeProcuracaoRepository procuracaoRepository,
                                 ClienteRepository clienteRepository,
                                 UsuarioRepository usuarioRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository);
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
    }

    void enforceRead(Usuario usuario, Processo processo) {
        if (usuario.getTipoUsuario() == TipoUsuario.CIDADAO) {
            authorizationService.requireReadProcessoAsCidadaoParte(processo);
            return;
        }
        authorizationService.requireReadProcesso(processo);
    }

    void enforceThreadAccess(Usuario usuario, AtendimentoThread thread) {
        if (usuario.getTipoUsuario() == TipoUsuario.CIDADAO) {
            Processo processo = processoRepository.findById(thread.getProcessoId()).orElseThrow();
            authorizationService.requireReadProcessoAsCidadaoParte(processo);
            if (!Objects.equals(usuario.getId(), thread.getCidadaoUsuarioId())) {
                throw new AccessDeniedException("Acesso negado");
            }
            return;
        }
        if (usuario.getTipoUsuario() == TipoUsuario.ADVOGADO) {
            if (!Objects.equals(usuario.getId(), thread.getAdvogadoId())) {
                throw new AccessDeniedException("Acesso negado");
            }
            boolean hasProcesso = procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatus(usuario.getId(), thread.getProcessoId(), LaianeProcuracaoStatus.ATIVA);
            if (!hasProcesso) {
                throw new AccessDeniedException("Acesso negado");
            }
            boolean isClient = clienteRepository.existsByCpfHashAndAdvogado_Id(thread.getCidadaoCpfHash(), usuario.getId());
            if (!isClient) {
                throw new AccessDeniedException("Acesso negado");
            }
            return;
        }
        throw new AccessDeniedException("Acesso negado");
    }

    Usuario resolveCidadao(AtendimentoCreateThreadRequest request) {
        if (request.cidadaoUsuarioId() != null) {
            return usuarioRepository.findById(request.cidadaoUsuarioId()).orElseThrow();
        }
        String cpf = AtendimentoChatSupportUtils.digitsOnly(request.cidadaoCpf());
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("cidadaoUsuarioId/cidadaoCpf");
        }
        return usuarioRepository.findByCpf(cpf).orElseThrow();
    }
}
