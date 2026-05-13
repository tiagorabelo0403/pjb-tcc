package com.tcc.pjb.backend.modules.advocacia.service;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.dto.ClienteDTO;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.mapper.ClienteMapper;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final ClienteMapper clienteMapper;
    private final ClienteSpecification clienteSpecification;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;

    public ClienteService(
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            MembroEquipeRepository membroEquipeRepository,
            ClienteMapper clienteMapper,
            ClienteSpecification clienteSpecification,
            CurrentUserService currentUserService,
            AuditLedgerService auditLedgerService
    ) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.membroEquipeRepository = membroEquipeRepository;
        this.clienteMapper = clienteMapper;
        this.clienteSpecification = clienteSpecification;
        this.currentUserService = currentUserService;
        this.auditLedgerService = auditLedgerService;
    }

    @Transactional(readOnly = true)
    public Page<ClienteDTO.ClienteResponse> buscarClientes(ClienteDTO.ClienteQuery query, Pageable pageable) {
        ClienteDTO.ClienteQuery effective = clampQueryToScope(query);
        Specification<Cliente> spec = clienteSpecification.build(effective);
        Page<Cliente> paginaDeClientes = clienteRepository.findAll(spec, pageable);
        return paginaDeClientes.map(clienteMapper::entidadeParaResponse);
    }

    @Transactional
    public ClienteDTO.ClienteResponse criarCliente(ClienteDTO.ClienteRequest dto) {
        Usuario advogado = resolveAdvogadoNoEscopo(dto.getAdvogadoId());

        Cliente novoCliente = clienteMapper.requestParaEntidade(dto, advogado);

        if (dto.getCpfCnpj() != null && !dto.getCpfCnpj().isBlank()) {
            String cpfNorm = CriptografiaPJB.normalizarDocumentoNumerico(dto.getCpfCnpj());
            novoCliente.setCpfCriptografado(cpfNorm);
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            novoCliente.setEmailCriptografado(dto.getEmail().trim().toLowerCase());
        }

        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();
        if (membroAtivo != null && membroAtivo.getEquipe() != null) {
            novoCliente.setEquipe(membroAtivo.getEquipe());
        } else {
            novoCliente.setEquipe(null);
        }

        String cpfCanonicalHash = CriptografiaPJB.hashCpfCnpj(dto.getCpfCnpj());
        if (cpfCanonicalHash != null) {
            novoCliente.setCpfHash(cpfCanonicalHash);
        }

        List<String> candidateHashes = CriptografiaPJB.candidateCpfHashes(dto.getCpfCnpj());
        if (!candidateHashes.isEmpty() && clienteRepository.existsByAdvogado_IdAndCpfHashIn(advogado.getId(), candidateHashes)) {
            throw new RecursoJaExistenteException("Cliente já cadastrado para este advogado.");
        }

        Cliente clienteSalvo = clienteRepository.save(novoCliente);
        auditLedgerService.appendSafely("ADV_CLIENTE_CREATED", "ADV_CLIENTE", String.valueOf(clienteSalvo.getId()), clienteSalvo.getCpfHash());
        return clienteMapper.entidadeParaResponse(clienteSalvo);
    }

    @Transactional(readOnly = true)
    public ClienteDTO.ClienteResponse buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));

        assertCanReadCliente(cliente);
        return clienteMapper.entidadeParaResponse(cliente);
    }

    @Transactional
    public ClienteDTO.ClienteResponse atualizarCliente(Long id, ClienteDTO.ClienteRequest dto) {
        Cliente clienteExistente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));

        assertCanManageCliente(clienteExistente);

        Usuario advogado = resolveAdvogadoNoEscopo(dto.getAdvogadoId());

        clienteMapper.atualizarEntidadeDoRequest(dto, clienteExistente);
        clienteExistente.setAdvogado(advogado);

        if (dto.getCpfCnpj() != null && !dto.getCpfCnpj().isBlank()) {
            String cpfNorm = CriptografiaPJB.normalizarDocumentoNumerico(dto.getCpfCnpj());
            clienteExistente.setCpfCriptografado(cpfNorm);
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            clienteExistente.setEmailCriptografado(dto.getEmail().trim().toLowerCase());
        }

        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();
        if (membroAtivo != null && membroAtivo.getEquipe() != null) {
            clienteExistente.setEquipe(membroAtivo.getEquipe());
        } else {
            clienteExistente.setEquipe(null);
        }

        String cpfCanonicalHash = CriptografiaPJB.hashCpfCnpj(dto.getCpfCnpj());
        if (cpfCanonicalHash != null) {
            clienteExistente.setCpfHash(cpfCanonicalHash);
        }

        List<String> candidateHashes = CriptografiaPJB.candidateCpfHashes(dto.getCpfCnpj());
        if (!candidateHashes.isEmpty() && clienteRepository.existsByAdvogado_IdAndCpfHashInAndIdNot(advogado.getId(), candidateHashes, id)) {
            throw new RecursoJaExistenteException("Cliente já cadastrado para este advogado.");
        }

        Cliente clienteSalvo = clienteRepository.save(clienteExistente);
        auditLedgerService.appendSafely("ADV_CLIENTE_UPDATED", "ADV_CLIENTE", String.valueOf(clienteSalvo.getId()), clienteSalvo.getCpfHash());
        return clienteMapper.entidadeParaResponse(clienteSalvo);
    }

    @Transactional
    public void excluirCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));

        assertCanManageCliente(cliente);

        cliente.setStatus(StatusCliente.ARQUIVADO);
        Cliente saved = clienteRepository.save(cliente);
        auditLedgerService.appendSafely("ADV_CLIENTE_ARCHIVED", "ADV_CLIENTE", String.valueOf(saved.getId()), saved.getCpfHash());
    }

    private ClienteDTO.ClienteQuery clampQueryToScope(ClienteDTO.ClienteQuery query) {
        Usuario current = currentUserService.getOrNull();
        if (current == null || current.getId() == null || !current.isAdvogado()) {
            return query;
        }

        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();

        if (membroAtivo == null || membroAtivo.getEquipe() == null || membroAtivo.getEquipe().getId() == null) {
            return ClienteDTO.ClienteQuery.builder()
                    .advogadoId(current.getId())
                    .nome(query != null ? query.getNome() : null)
                    .cpfCnpj(query != null ? query.getCpfCnpj() : null)
                    .status(query != null ? query.getStatus() : null)
                    .cadastradoDepois(query != null ? query.getCadastradoDepois() : null)
                    .build();
        }

        boolean canManageOthers = papelPodeGerenciar(membroAtivo.getPapel());
        Long requestedAdvId = query != null ? query.getAdvogadoId() : null;
        Long effectiveAdvId = requestedAdvId;

        if (!canManageOthers) {
            effectiveAdvId = current.getId();
        }

        return ClienteDTO.ClienteQuery.builder()
                .advogadoId(effectiveAdvId)
                .nome(query != null ? query.getNome() : null)
                .cpfCnpj(query != null ? query.getCpfCnpj() : null)
                .status(query != null ? query.getStatus() : null)
                .cadastradoDepois(query != null ? query.getCadastradoDepois() : null)
                .build();
    }

    private void assertCanReadCliente(Cliente cliente) {
        Usuario current = currentUserService.getOrNull();
        if (current == null || current.getId() == null || !current.isAdvogado()) {
            return;
        }

        if (cliente.getEquipe() == null || cliente.getEquipe().getId() == null) {
            if (!Objects.equals(cliente.getAdvogado().getId(), current.getId())) {
                throw new AccessDeniedPjbException("cliente_fora_do_escopo");
            }
            return;
        }

        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();
        if (membroAtivo == null || membroAtivo.getEquipe() == null || membroAtivo.getEquipe().getId() == null) {
            throw new AccessDeniedPjbException("cliente_requer_contexto_de_equipe");
        }

        if (!Objects.equals(cliente.getEquipe().getId(), membroAtivo.getEquipe().getId())) {
            throw new AccessDeniedPjbException("cliente_fora_da_equipe_ativa");
        }
    }

    private void assertCanManageCliente(Cliente cliente) {
        Usuario current = currentUserService.getOrNull();
        if (current == null || current.getId() == null || !current.isAdvogado()) {
            return;
        }

        if (cliente.getEquipe() == null || cliente.getEquipe().getId() == null) {
            if (!Objects.equals(cliente.getAdvogado().getId(), current.getId())) {
                throw new AccessDeniedPjbException("cliente_fora_do_escopo");
            }
            return;
        }

        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();
        if (membroAtivo == null || membroAtivo.getEquipe() == null || membroAtivo.getEquipe().getId() == null) {
            throw new AccessDeniedPjbException("cliente_requer_contexto_de_equipe");
        }

        if (!Objects.equals(cliente.getEquipe().getId(), membroAtivo.getEquipe().getId())) {
            throw new AccessDeniedPjbException("cliente_fora_da_equipe_ativa");
        }

        if (Objects.equals(cliente.getAdvogado().getId(), current.getId())) {
            return;
        }

        if (!papelPodeGerenciar(membroAtivo.getPapel())) {
            throw new AccessDeniedPjbException("papel_nao_autorizado_a_gerenciar_clientes_do_escritorio");
        }
    }

    private Usuario resolveAdvogadoNoEscopo(Long advogadoId) {
        if (advogadoId == null) {
            throw new AccessDeniedPjbException("advogado_id_ausente");
        }

        Usuario current = currentUserService.getRequired();
        if (current.getId() == null) {
            throw new AccessDeniedPjbException("usuario_sem_id");
        }

        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();

        if (membroAtivo == null || membroAtivo.getEquipe() == null || membroAtivo.getEquipe().getId() == null) {
            if (!advogadoId.equals(current.getId())) {
                throw new AccessDeniedPjbException("escopo_independente_nao_permite_operar_em_nome_de_outro_advogado");
            }
            return current;
        }

        long equipeId = membroAtivo.getEquipe().getId();
        if (advogadoId.equals(current.getId())) {
            return current;
        }

        if (!papelPodeGerenciar(membroAtivo.getPapel())) {
            throw new AccessDeniedPjbException("papel_nao_autorizado_a_gerenciar_clientes_do_escritorio");
        }

        if (!membroEquipeRepository.existsByUsuario_IdAndEquipe_IdAndAtivoTrue(advogadoId, equipeId)) {
            throw new AccessDeniedPjbException("advogado_alvo_nao_pertence_ao_escritorio_ativo");
        }

        return usuarioRepository.findById(advogadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Advogado", advogadoId));
    }

    private boolean papelPodeGerenciar(PapelEquipe papel) {
        if (papel == null) {
            return false;
        }
        return papel == PapelEquipe.ADMINISTRADOR || papel == PapelEquipe.COORDENADOR;
    }
}
