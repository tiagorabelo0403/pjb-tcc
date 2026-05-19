package com.tcc.pjb.backend.modules.acordo.infrastructure;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.acordo.api.UsuarioContextoAcordo;
import com.tcc.pjb.backend.modules.acordo.api.UsuarioAcordoPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PjbUsuarioAcordoAdapter implements UsuarioAcordoPort {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;

    public PjbUsuarioAcordoAdapter(ProcessoRepository processoRepository,
                                   UsuarioRepository usuarioRepository) {
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean existeUsuario(Long usuarioId) {
        return usuarioId != null && usuarioRepository.existsById(usuarioId);
    }

    @Override
    public boolean usuarioPodeParticipar(Long processoId, Long usuarioId) {
        if (processoId == null || usuarioId == null) {
            return false;
        }
        Processo processo = processoRepository.findContextoCompletoById(processoId).orElse(null);
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (processo == null || usuario == null || !usuario.isAtivo()) {
            return false;
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo != null && (tipo.isMagistratura()
                || tipo.isConciliacaoMediacao()
                || tipo.isServidorJudiciario()
                || tipo.isAdministradorSistema())) {
            return true;
        }
        if (Objects.equals(processo.getUsuario() != null ? processo.getUsuario().getId() : null, usuarioId)) {
            return true;
        }
        String cpf = digits(usuario.getCpf());
        return !cpf.isBlank()
                && (cpf.equals(digits(processo.getParteAutoraCpf())) || cpf.equals(digits(processo.getParteReuCpf())));
    }

    @Override
    public boolean usuarioPodeHomologar(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .map(Usuario::isMagistrado)
                .orElse(false);
    }

    @Override
    public UsuarioContextoAcordo obterContextoUsuario(Long processoId, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        return new UsuarioContextoAcordo(
                usuarioId,
                nomeExibicao(usuario),
                papeis(usuario),
                usuario.isAtivo(),
                usuarioPodeParticipar(processoId, usuarioId),
                usuarioPodeHomologar(usuarioId)
        );
    }

    private String nomeExibicao(Usuario usuario) {
        String nome = usuario.getNome();
        if (nome != null && !nome.isBlank()) {
            return nome.trim();
        }
        String username = usuario.getUsername();
        return username == null || username.isBlank() ? "Usuario " + usuario.getId() : username.trim();
    }

    private List<String> papeis(Usuario usuario) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        List<String> out = new ArrayList<>();
        if (tipo != null) {
            out.add(tipo.name());
            out.add(tipo.papelArquitetural());
        }
        if (usuario.isAdvogado()) {
            out.add("ADVOGADO");
        }
        if (usuario.isMagistrado()) {
            out.add("MAGISTRATURA");
        }
        if (usuario.isServidorJudiciario()) {
            out.add("SERVIDOR_JUDICIARIO");
        }
        return out.stream().distinct().toList();
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D+", "");
    }
}
