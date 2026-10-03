package com.tcc.pjb.backend.core.comunicacao.institucional.entry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalEntryContextApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalEntryContextResolverService contextResolver;
    private final InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver;

    public InstitutionalEntryContextApplicationService(CurrentUserService currentUserService,
                                                       InstitutionalEntryContextResolverService contextResolver,
                                                       InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.contextResolver = Objects.requireNonNull(contextResolver);
        this.identityBaseProfileResolver = Objects.requireNonNull(identityBaseProfileResolver);
    }

    public InstitutionalEntrySummary resolverEntradaAtual() {
        Usuario usuario = currentUserService.getRequired();
        List<InstitutionalEntryContext> contextos = contextResolver.resolverContextos(usuario);
        InstitutionalEntryContext preferencial = contextos.stream().findFirst().orElse(null);
        InstitutionalIdentityBaseProfile identidadeBase = identityBaseProfileResolver.resolve(usuario);
        return new InstitutionalEntrySummary(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario(),
                identidadeBase,
                possuiAmbientePessoal(usuario),
                !contextos.isEmpty(),
                contextos,
                preferencial,
                Instant.now()
        );
    }

    public List<InstitutionalEntryContext> resolverContextosAtuais() {
        return contextResolver.resolverContextos(currentUserService.getRequired());
    }

    private boolean possuiAmbientePessoal(Usuario usuario) {
        return identityBaseProfileResolver.resolve(usuario).possuiFluxoDireto();
    }
}
