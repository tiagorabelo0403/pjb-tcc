package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialGeracaoContextResolverService {

    private final CalculoJudicialSolicitanteResolverService solicitanteResolverService;

    public CalculoJudicialGeracaoContextResolverService(CalculoJudicialSolicitanteResolverService solicitanteResolverService) {
        this.solicitanteResolverService = Objects.requireNonNull(solicitanteResolverService);
    }

    public CalculoJudicialGeracaoContext resolve(Authentication authentication,
                                                 CalculoJudicialSolicitantePerfil perfil,
                                                 String nomeSolicitante,
                                                 String registroProfissionalSolicitante,
                                                 String dominio,
                                                 String titulo,
                                                 String numeroProcesso,
                                                 BigDecimal totalGeral,
                                                 Instant geradoEm) {
        CalculoJudicialSolicitanteContext solicitante = solicitanteResolverService.resolve(authentication, perfil, nomeSolicitante, registroProfissionalSolicitante);
        MembroEquipe membroEquipe = EquipeContexto.getMembroDaEquipeAtiva();
        Equipe equipe = membroEquipe != null ? membroEquipe.getEquipe() : null;
        Long equipeId = equipe != null ? equipe.getId() : null;
        String equipeNome = normalize(equipe != null ? equipe.getNome() : null);
        String equipeRotulo = equipeNome == null ? null : profileTeamLabel(perfil);
        String hash = sha256(
                blank(dominio),
                blank(titulo),
                blank(numeroProcesso),
                blank(perfil != null ? perfil.name() : null),
                CalculoJudicialMetadataSupport.money(totalGeral),
                geradoEm != null ? geradoEm.toString() : "",
                solicitante.nome(),
                blank(solicitante.registro()),
                blank(equipeNome),
                equipeId != null ? String.valueOf(equipeId) : ""
        );
        return new CalculoJudicialGeracaoContext(
                solicitante.nome(),
                solicitante.registro(),
                solicitante.rotulo(),
                solicitante.nomeArquivo(),
                equipeId,
                equipeNome,
                equipeRotulo,
                hash
        );
    }

    private String profileTeamLabel(CalculoJudicialSolicitantePerfil perfil) {
        if (perfil == null) {
            return "Equipe ativa";
        }
        return switch (perfil) {
            case ADVOGADO -> "Escritório ou equipe ativa";
            case PROCURADORIA, TECNICO_INSTITUCIONAL -> "Unidade ou equipe ativa";
            case MAGISTRATURA -> "Gabinete ou equipe ativa";
            case CONTADOR_JUDICIAL -> "Setor ou equipe ativa";
            case CIDADAO -> "Equipe institucional ativa";
        };
    }

    private String sha256(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update(blank(value).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '|');
            }
            return HexFormat.of().formatHex(digest.digest()).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("calculo_judicial_hash_algorithm_not_available", ex);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String blank(String value) {
        return value == null ? "" : value;
    }
}
