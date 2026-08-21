package com.tcc.pjb.backend.service.magistratura;

import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.OrigemAutenticacaoSessao;
import com.tcc.pjb.backend.model.entity.enums.SituacaoConta;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MagistradoAtivacaoService {

    private final UsuarioRepository usuarioRepository;
    private final SecurityChallengeService securityChallengeService;
    private final PasskeySessionService passkeySessionService;

    public MagistradoAtivacaoService(UsuarioRepository usuarioRepository,
                                      SecurityChallengeService securityChallengeService,
                                      PasskeySessionService passkeySessionService) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.securityChallengeService = Objects.requireNonNull(securityChallengeService);
        this.passkeySessionService = Objects.requireNonNull(passkeySessionService);
    }

    @Transactional
    public PasskeySessionService.IssuedPasskeySession confirmarAtivacao(Long usuarioId, Long challengeId, String codigo, String ip) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Usuário não encontrado."));

        if (usuario.getSituacaoConta() != SituacaoConta.PENDENTE_ATIVACAO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Conta não está pendente de ativação.");
        }

        securityChallengeService.consumeOtp(challengeId, usuario, codigo);

        usuario.setSituacaoConta(SituacaoConta.ATIVA);
        Usuario salvo = usuarioRepository.save(usuario);

        return passkeySessionService.issue(salvo, null, ip, OrigemAutenticacaoSessao.PASSKEY);
    }
}
