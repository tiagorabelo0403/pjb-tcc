package com.tcc.pjb.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.CredencialAcesso;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.CredencialAcessoRepository;

@Service
public class CredencialAcessoService {

    private final CredencialAcessoRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public CredencialAcessoService(CredencialAcessoRepository repository) {
        this.repository = repository;
    }

    
    public CredencialAcesso gerarCredencial(Processo processo) {

        String login = "PROC-" + processo.getNumeroProcesso();
        String senhaPlana = UUID.randomUUID().toString().substring(0, 10);

        CredencialAcesso credencial = CredencialAcesso.builder()
                .login(login)
                .senhaHash(encoder.encode(senhaPlana))
                .validade(LocalDateTime.now().plusDays(30))
                .ativa(true)
                .processo(processo)
                .build();

        return repository.save(credencial);
    }
}
