package com.tcc.pjb.backend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.core.validation.oab.OabStrictValidator;
import com.tcc.pjb.backend.mapper.UsuarioMapper;
import com.tcc.pjb.backend.model.dto.UsuarioRequest;
import com.tcc.pjb.backend.model.dto.UsuarioResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final OabStrictValidator oabStrictValidator;
    private final DocumentoNacionalValidator documentoNacionalValidator;
    private final IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService;
    private final AuditLedgerService auditLedgerService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          UsuarioMapper usuarioMapper,
                          OabStrictValidator oabStrictValidator,
                          DocumentoNacionalValidator documentoNacionalValidator,
                          IdentidadeJuridicaNacionalService identidadeJuridicaNacionalService,
                          AuditLedgerService auditLedgerService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.oabStrictValidator = oabStrictValidator;
        this.documentoNacionalValidator = documentoNacionalValidator;
        this.identidadeJuridicaNacionalService = identidadeJuridicaNacionalService;
        this.auditLedgerService = auditLedgerService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodosUsuarios() {
        List<Usuario> entidades = usuarioRepository.findAll();
        List<UsuarioResponse> responses = usuarioMapper.entidadeParaResponseLista(entidades);
        enrichResponses(entidades, responses);
        return responses;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario entidade = buscarUsuarioPeloId(id);
        UsuarioResponse response = usuarioMapper.entidadeParaResponse(entidade);
        enrichResponse(entidade, response);
        return response;
    }

    @Transactional
    public UsuarioResponse criarUsuario(UsuarioRequest dto) {
        String cpfNormalizado = normalizarCpfObrigatorio(dto.getCpf());
        validarEmailECpf(dto.getEmail(), cpfNormalizado);

        Usuario novaEntidade = usuarioMapper.requestParaEntidade(dto);
        novaEntidade.setCpf(cpfNormalizado);
        aplicarTipoUsuarioSeAusente(novaEntidade);
        aplicarValidacaoOabStrict(dto, novaEntidade, null);

        Usuario entidadeSalva = usuarioRepository.save(novaEntidade);
        sincronizarIdentidadeNacional(entidadeSalva);
        Usuario entidadeFinal = usuarioRepository.save(entidadeSalva);
        auditLedgerService.appendSafely("USUARIO_CRIADO", "USUARIO", String.valueOf(entidadeFinal.getId()),
                null, "por:" + obterAtorAtual());

        UsuarioResponse response = usuarioMapper.entidadeParaResponse(entidadeFinal);
        enrichResponse(entidadeFinal, response);
        return response;
    }

    @Transactional
    public UsuarioResponse atualizarUsuario(Long id, UsuarioRequest dto) {
        Usuario entidadeExistente = buscarUsuarioPeloId(id);
        validarEmailECpfSeAlterado(entidadeExistente, dto);

        usuarioMapper.atualizarEntidadeDoRequest(dto, entidadeExistente);
        if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
            entidadeExistente.setCpf(normalizarCpfObrigatorio(dto.getCpf()));
        }
        aplicarTipoUsuarioSeAusente(entidadeExistente);
        aplicarValidacaoOabStrict(dto, entidadeExistente, id);

        Usuario entidadeSalva = usuarioRepository.save(entidadeExistente);
        sincronizarIdentidadeNacional(entidadeSalva);
        Usuario entidadeFinal = usuarioRepository.save(entidadeSalva);
        auditLedgerService.appendSafely("USUARIO_PERFIL_ALTERADO", "USUARIO", String.valueOf(id),
                null, "por:" + obterAtorAtual());

        UsuarioResponse response = usuarioMapper.entidadeParaResponse(entidadeFinal);
        enrichResponse(entidadeFinal, response);
        return response;
    }

    @Transactional
    public void desativarUsuario(Long id) {
        Usuario entidade = buscarUsuarioPeloId(id);
        entidade.setAtivo(false);
        usuarioRepository.save(entidade);
        auditLedgerService.appendSafely("USUARIO_DESATIVADO", "USUARIO", String.valueOf(id),
                null, "por:" + obterAtorAtual());
    }

    private String obterAtorAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "SISTEMA";
    }

    private Usuario buscarUsuarioPeloId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));
    }

    private void validarEmailECpf(String email, String cpf) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RecursoJaExistenteException("Email já cadastrado: " + email);
        }
        if (usuarioRepository.findByCpf(cpf).isPresent()) {
            throw new RecursoJaExistenteException("CPF já cadastrado: " + cpf);
        }
    }

    private void validarEmailECpfSeAlterado(Usuario existente, UsuarioRequest dto) {
        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !dto.getEmail().equalsIgnoreCase(existente.getEmail())) {
            if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new RecursoJaExistenteException("Email já cadastrado: " + dto.getEmail());
            }
        }
        if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
            String cpfNormalizado = normalizarCpfObrigatorio(dto.getCpf());
            if (!cpfNormalizado.equals(existente.getCpf()) && usuarioRepository.findByCpf(cpfNormalizado).isPresent()) {
                throw new RecursoJaExistenteException("CPF já cadastrado: " + cpfNormalizado);
            }
        }
    }

    private void aplicarValidacaoOabStrict(UsuarioRequest dto, Usuario usuario, Long existingIdOrNull) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null && usuario.getPerfil() != null) {
            tipo = TipoUsuario.fromPerfil(usuario.getPerfil());
        }

        boolean needsOab = tipo == TipoUsuario.ADVOGADO || tipo == TipoUsuario.OAB_PRESIDENTE_SECCIONAL;
        String raw = dto != null ? dto.getOab() : null;

        if (!needsOab) {
            usuario.setOab(null);
            usuario.setOabNormalizada(null);
            usuario.setOabUf(null);
            usuario.setOabNumero(null);
            usuario.setOabSufixo(null);
            return;
        }

        String effective = raw != null && !raw.isBlank() ? raw : usuario.getOab();
        if (effective == null || effective.isBlank()) {
            throw new IllegalArgumentException("OAB é obrigatória para perfis de advocacia.");
        }

        var info = oabStrictValidator.parseAndValidate(effective);
        usuario.setOab(info.canonicalDisplay());
        usuario.setOabNormalizada(info.normalized());
        usuario.setOabUf(info.uf());
        usuario.setOabNumero(info.numero());
        usuario.setOabSufixo(info.sufixo());

        if (info.normalized() != null && !info.normalized().isBlank()) {
            var existing = usuarioRepository.findByOabNormalizada(info.normalized());
            if (existing.isPresent() && (existingIdOrNull == null || !existing.get().getId().equals(existingIdOrNull))) {
                throw new RecursoJaExistenteException("OAB já cadastrada no sistema: " + info.normalized());
            }
        }
    }

    private void aplicarTipoUsuarioSeAusente(Usuario usuario) {
        if (usuario.getTipoUsuario() == null && usuario.getPerfil() != null) {
            usuario.setTipoUsuario(TipoUsuario.fromPerfil(usuario.getPerfil()));
        }
    }

    private void sincronizarIdentidadeNacional(Usuario usuario) {
        var identidade = identidadeJuridicaNacionalService.sincronizarUsuario(usuario);
        usuario.setIdentidadeJuridicaId(identidade.getId());
    }

    private void enrichResponses(List<Usuario> entidades, List<UsuarioResponse> responses) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (Usuario entidade : entidades) {
            if (entidade.getIdentidadeJuridicaId() != null) {
                ids.add(entidade.getIdentidadeJuridicaId());
            }
        }
        Map<UUID, IdentidadeJuridicaNacionalService.IdentidadeResumo> resumoPorId = identidadeJuridicaNacionalService.resumirPorIds(ids);
        int max = Math.min(entidades.size(), responses.size());
        for (int i = 0; i < max; i++) {
            Usuario entidade = entidades.get(i);
            UsuarioResponse response = responses.get(i);
            applyIdentitySummary(response, resumoPorId.get(entidade.getIdentidadeJuridicaId()), entidade.getIdentidadeJuridicaId());
        }
    }

    private void enrichResponse(Usuario entidade, UsuarioResponse response) {
        IdentidadeJuridicaNacionalService.IdentidadeResumo resumo = identidadeJuridicaNacionalService
                .resumirPorId(entidade.getIdentidadeJuridicaId())
                .orElse(null);
        applyIdentitySummary(response, resumo, entidade.getIdentidadeJuridicaId());
    }

    private void applyIdentitySummary(UsuarioResponse response,
                                      IdentidadeJuridicaNacionalService.IdentidadeResumo resumo,
                                      UUID identidadeJuridicaId) {
        response.setIdentidadeJuridicaId(identidadeJuridicaId);
        if (resumo == null) {
            return;
        }
        response.setDocumentoMascarado(resumo.documentoMascarado());
        response.setNomeCanonicoIdentidade(resumo.nomeCanonico());
        response.setProntuarioNacionalUri(resumo.prontuarioNacionalUri());
        response.setNivelConfiancaIdentidade(resumo.nivelConfianca());
        response.setReceitaStatusIdentidade(resumo.receitaStatus());
        response.setOabStatusIdentidade(resumo.oabStatus());
        response.setGovBrVinculado(resumo.govBrVinculado());
    }

    private String normalizarCpfObrigatorio(String cpf) {
        String digits = documentoNacionalValidator.normalizarDocumento(cpf);
        if (!documentoNacionalValidator.cpfValido(digits)) {
            throw new IllegalArgumentException("CPF inválido: " + cpf);
        }
        return digits;
    }
}
