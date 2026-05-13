package com.tcc.pjb.backend.service.processual.malha.internal;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaActorContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaAuthorizationService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;

    public ProcessoMalhaAuthorizationService(CurrentUserService currentUserService,
                                             ProcessoRepository processoRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional(readOnly = true)
    public ProcessoMalhaActorContext resolver(Long processoId, String papelSolicitado, String ramoSolicitado) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = processoRepository.findContextoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        TipoUsuario tipoReal = usuario.getTipoUsuario() == null
                ? fallbackTipoUsuario(usuario)
                : usuario.getTipoUsuario();
        TipoUsuario papelPedido = TipoUsuario.fromPerfil(papelSolicitado);
        boolean visualizacaoElevada = possuiVisualizacaoElevada(tipoReal);
        boolean parteRelacionada = relacionaComProcesso(usuario, processo);
        boolean visualizacaoContextual = visualizacaoElevada || parteRelacionada || tipoReal.isAdvocacia() || tipoReal.isAuxiliarJustica() || tipoReal.isMinisterioPublico() || tipoReal.isDefensoriaPublica() || tipoReal.isProcuradoria();
        if (!visualizacaoContextual) {
            throw new AccessDeniedException("Acesso contextual à malha não autorizado para o processo informado");
        }
        TipoUsuario papelEfetivo = resolverPapelEfetivo(tipoReal, papelPedido, visualizacaoElevada);
        RamoDireito ramoEfetivo = resolverRamoEfetivo(processo, ramoSolicitado, visualizacaoElevada);
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        roles.add(tipoReal.name());
        if (usuario.getPerfil() != null && !usuario.getPerfil().isBlank()) {
            roles.add(usuario.getPerfil().trim().toUpperCase(Locale.ROOT));
        }
        if (visualizacaoElevada) {
            roles.add("MALHA_VISUALIZACAO_ELEVADA");
        }
        if (parteRelacionada) {
            roles.add("MALHA_PARTE_RELACIONADA");
        }
        return new ProcessoMalhaActorContext(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                tipoReal,
                papelEfetivo,
                ramoEfetivo,
                List.copyOf(roles),
                visualizacaoElevada,
                visualizacaoContextual,
                parteRelacionada
        );
    }

    public boolean canAccessRequestedRole(String papelSolicitado) {
        Usuario usuario = currentUserService.getOptional().orElse(null);
        if (usuario == null) {
            return false;
        }
        TipoUsuario tipoReal = usuario.getTipoUsuario() == null ? fallbackTipoUsuario(usuario) : usuario.getTipoUsuario();
        TipoUsuario pedido = TipoUsuario.fromPerfil(papelSolicitado);
        if (pedido == null) {
            return true;
        }
        return possuiVisualizacaoElevada(tipoReal) || pedido == tipoReal;
    }

    private TipoUsuario resolverPapelEfetivo(TipoUsuario tipoReal,
                                             TipoUsuario papelPedido,
                                             boolean visualizacaoElevada) {
        if (papelPedido == null) {
            return tipoReal;
        }
        if (papelPedido == tipoReal) {
            return papelPedido;
        }
        if (visualizacaoElevada) {
            return papelPedido;
        }
        return tipoReal;
    }

    private RamoDireito resolverRamoEfetivo(Processo processo,
                                            String ramoSolicitado,
                                            boolean visualizacaoElevada) {
        RamoDireito pedido = RamoDireito.fromNullable(ramoSolicitado);
        if (pedido == null) {
            return processo.getRamoDireito();
        }
        if (visualizacaoElevada) {
            return pedido;
        }
        return processo.getRamoDireito() == null ? pedido : processo.getRamoDireito();
    }

    private boolean possuiVisualizacaoElevada(TipoUsuario tipoUsuario) {
        return tipoUsuario.isMagistratura()
                || tipoUsuario.isAssessor()
                || tipoUsuario.isServidorJudiciario()
                || tipoUsuario.isAdministradorSistema()
                || tipoUsuario.isMinisterioPublico()
                || tipoUsuario.isDefensoriaPublica()
                || tipoUsuario.isProcuradoria();
    }

    private boolean relacionaComProcesso(Usuario usuario, Processo processo) {
        String cpf = normalizar(usuario.getCpf());
        if (cpf.isBlank()) {
            return false;
        }
        if (Objects.equals(cpf, normalizar(processo.getParteAutoraCpf()))) {
            return true;
        }
        if (Objects.equals(cpf, normalizar(processo.getParteReuCpf()))) {
            return true;
        }
        return processo.getUsuario() != null && Objects.equals(cpf, normalizar(processo.getUsuario().getCpf()));
    }

    private TipoUsuario fallbackTipoUsuario(Usuario usuario) {
        TipoUsuario perfil = TipoUsuario.fromPerfil(usuario.getPerfil());
        if (perfil != null) {
            return perfil;
        }
        if (usuario.isAdvogado()) {
            return TipoUsuario.ADVOGADO;
        }
        return TipoUsuario.CIDADAO;
    }

    private String normalizar(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
