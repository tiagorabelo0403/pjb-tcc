package com.tcc.pjb.backend.core.security.identity;

import com.tcc.pjb.backend.core.icp.IcpBrasilCertProfile;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Resolve a identidade de usuário a partir do perfil de certificado ICP-Brasil já validado.
 * Esta classe não valida cadeia, não persiste estado e não executa auditoria.
 */
@Service
public class CertificadoIdentidadeResolver {

    private final UsuarioRepository usuarioRepository;

    public CertificadoIdentidadeResolver(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
    }

    public IdentidadeResolucao resolver(IcpBrasilCertProfile profile) {
        String cpf = normalizarCpf(profile == null ? null : profile.cpfTitular());
        if (cpf == null) {
            return new IdentidadeNaoResolvida(MotivoIdentidade.CPF_AUSENTE);
        }
        return usuarioRepository.findByCpf(cpf)
                .<IdentidadeResolucao>map(this::resolverUsuario)
                .orElseGet(() -> new IdentidadeNaoResolvida(MotivoIdentidade.USUARIO_INEXISTENTE));
    }

    private IdentidadeResolucao resolverUsuario(Usuario usuario) {
        if (!usuario.isAtivo()) {
            return new IdentidadeNaoResolvida(MotivoIdentidade.USUARIO_INATIVO);
        }
        return new IdentidadeResolvida(usuario);
    }

    private static String normalizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String normalizado = cpf.replaceAll("\\D", "");
        return normalizado.length() == 11 ? normalizado : null;
    }
}
