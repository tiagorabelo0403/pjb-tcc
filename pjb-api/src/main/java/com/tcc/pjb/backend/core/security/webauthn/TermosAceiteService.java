package com.tcc.pjb.backend.core.security.webauthn;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.entity.security.TermosAceite;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import com.tcc.pjb.backend.model.repository.security.TermosAceiteRepository;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermosAceiteService {

    private final TermosAceiteRepository repository;
    private final PasskeySessionRepository passkeySessionRepository;
    private final String versaoAtual;

    public TermosAceiteService(TermosAceiteRepository repository,
                               PasskeySessionRepository passkeySessionRepository,
                               @Value("${pjb.security.termos.versao-atual:v1}") String versaoAtual) {
        this.repository = Objects.requireNonNull(repository);
        this.passkeySessionRepository = Objects.requireNonNull(passkeySessionRepository);
        this.versaoAtual = Objects.requireNonNull(versaoAtual);
    }

    public String versaoAtual() {
        return versaoAtual;
    }

    @Transactional(readOnly = true)
    public boolean precisaAceitar(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return true;
        }
        return repository.findByUsuarioIdAndVersao(usuario.getId(), versaoAtual).isEmpty();
    }

    @Transactional
    public void registrarAceite(Usuario usuario, String versao, String ip) {
        Objects.requireNonNull(usuario, "usuario");
        String versaoNormalizada = (versao == null || versao.isBlank()) ? versaoAtual : versao.trim();
        if (!versaoNormalizada.equals(versaoAtual)) {
            throw new IllegalArgumentException("Versão de termos inválida ou desatualizada: " + versaoNormalizada);
        }
        if (repository.findByUsuarioIdAndVersao(usuario.getId(), versaoNormalizada).isPresent()) {
            return;
        }
        repository.save(new TermosAceite(usuario, versaoNormalizada, ip));
    }

    @Transactional
    public void confirmarAceitePorToken(String token, String versao, String ip) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token pendente ausente.");
        }
        String hash = PasskeySessionService.sha256Hex(token.trim());
        PasskeySession sessao = passkeySessionRepository.findActiveByTokenHash(hash)
                .orElseThrow(() -> new IllegalStateException("Sessão pendente não encontrada ou já revogada."));
        if (sessao.isExpired()) {
            throw new IllegalStateException("Sessão pendente expirada.");
        }
        if (!sessao.isTermosPendentes()) {
            throw new IllegalStateException("Sessão não está pendente de aceite de termos.");
        }
        registrarAceite(sessao.getUsuario(), versao, ip);
        sessao.setTermosPendentes(false);
        passkeySessionRepository.save(sessao);
    }
}
