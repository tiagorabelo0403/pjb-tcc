package com.tcc.pjb.backend.core.kernel.recursal.mesh.rules;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AdmissibilityDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInstrumento;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInstrumentoTrabalhista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoPeticao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRegimental;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ConflitoCompetencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.CorrecaoParcial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucaoFiscal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosTerceiro;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreparoDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreventionDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ReclamacaoConstitucional;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhadoResolver;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalPerfilReal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalPerfilRealCatalog;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoInominadoJuizado;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PedidoUniformizacaoFederal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoOrdinarioConstitucional;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoOrdinarioTrabalhista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoRevista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoRevista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RemessaDisposition;
import com.tcc.pjb.backend.domain.enums.TipoJustica;

abstract class AbstractRecursalRuleProfile implements RecursalRuleProfile {

    private final EnumSet<RecursalTribunal> supported;
    private final RecursalTribunalPerfilRealCatalog perfisReais;
    private final RecursalTribunalDetalhadoResolver tribunalDetalhadoResolver;

    protected AbstractRecursalRuleProfile(RecursalTribunal... supported) {
        this.supported = EnumSet.noneOf(RecursalTribunal.class);
        this.supported.addAll(Arrays.asList(supported));
        this.perfisReais = RecursalTribunalPerfilRealCatalog.defaultCatalog();
        this.tribunalDetalhadoResolver = new RecursalTribunalDetalhadoResolver();
    }

    @Override
    public boolean supports(RecursalTribunal tribunal) {
        return supported.contains(tribunal);
    }

    protected final RecursalRoutePlan sameCourtPlan(
            RecursalCaseContext context,
            RecursalAuthority merit,
            PreparoDisposition preparo,
            AdmissibilityDisposition admissibilidade,
            PreventionDisposition prevencao) {
        return sameCourtPlan(name(), context, merit, preparo, admissibilidade, prevencao);
    }

    protected final RecursalRoutePlan sameCourtPlan(
            String profileName,
            RecursalCaseContext context,
            RecursalAuthority merit,
            PreparoDisposition preparo,
            AdmissibilityDisposition admissibilidade,
            PreventionDisposition prevencao) {
        return new RecursalRoutePlan(
                profileName,
                context.tribunalOrigem(),
                tribunalDetalhadoResolver.resolveOrigem(context),
                admissibilidade.autoridadeOrigem(),
                context.tribunalOrigem(),
                tribunalDetalhadoResolver.resolveDestino(context, context.tribunalOrigem()),
                context.instanciaAtual(),
                admissibilidade.autoridadeDestino(),
                merit,
                preparo,
                admissibilidade,
                prevencao,
                RemessaDisposition.internaMesmosAutos()
        );
    }

    protected final RecursalRoutePlan externalPlan(
            RecursalCaseContext context,
            RecursalTribunal tribunalDestino,
            InstanceLevel instanciaDestino,
            RecursalAuthority merit,
            PreparoDisposition preparo,
            AdmissibilityDisposition admissibilidade,
            PreventionDisposition prevencao) {
        return externalPlan(name(), context, tribunalDestino, instanciaDestino, merit, preparo, admissibilidade, prevencao);
    }

    protected final RecursalRoutePlan externalPlan(
            String profileName,
            RecursalCaseContext context,
            RecursalTribunal tribunalDestino,
            InstanceLevel instanciaDestino,
            RecursalAuthority merit,
            PreparoDisposition preparo,
            AdmissibilityDisposition admissibilidade,
            PreventionDisposition prevencao) {
        return new RecursalRoutePlan(
                profileName,
                context.tribunalOrigem(),
                tribunalDetalhadoResolver.resolveOrigem(context),
                admissibilidade.autoridadeOrigem(),
                tribunalDestino,
                tribunalDetalhadoResolver.resolveDestino(context, tribunalDestino),
                instanciaDestino,
                admissibilidade.autoridadeDestino(),
                merit,
                preparo,
                admissibilidade,
                prevencao,
                RemessaDisposition.externaDistribuicaoMesmaNumeracao()
        );
    }

    protected final RecursalRoutePlan externalAutonomousPlan(
            String profileName,
            RecursalCaseContext context,
            RecursalTribunal tribunalDestino,
            InstanceLevel instanciaDestino,
            RecursalAuthority merit,
            PreparoDisposition preparo,
            AdmissibilityDisposition admissibilidade,
            PreventionDisposition prevencao) {
        return new RecursalRoutePlan(
                profileName,
                context.tribunalOrigem(),
                tribunalDetalhadoResolver.resolveOrigem(context),
                admissibilidade.autoridadeOrigem(),
                tribunalDestino,
                tribunalDetalhadoResolver.resolveDestino(context, tribunalDestino),
                instanciaDestino,
                admissibilidade.autoridadeDestino(),
                merit,
                preparo,
                admissibilidade,
                prevencao,
                RemessaDisposition.externaAutuacaoDistribuicao()
        );
    }

    protected final RecursalTribunalPerfilReal realProfile(RecursalCaseContext context) {
        return perfisReais.profileOf(context);
    }

    protected final void require(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    protected final void requireSecondInstance(RecursalCaseContext context) {
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE, "A espécie exige acórdão de segundo grau");
    }

    protected final void requireFirstInstance(RecursalCaseContext context) {
        require(context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE, "A espécie exige decisão de primeiro grau");
    }

    protected final void requireMonocraticDecision(RecursalCaseContext context) {
        require(context.decisaoMonocratica() || context.autoridadeAtual().decisaoMonocratica(), "A espécie exige decisão monocrática antecedente");
    }

    protected final void requireColegiateDecision(RecursalCaseContext context) {
        require(context.acordaoColegiado() || context.autoridadeAtual().colegiado(), "A espécie exige acórdão colegiado antecedente");
    }

    protected final PreparoDisposition ordinaryCivilPreparo(RecursalCaseContext context) {
        if (context.justicaGratuitaOuIsencaoLegal() || context.fazendaPublicaOuMp()) {
            return PreparoDisposition.dispensado();
        }
        return PreparoDisposition.obrigatorio(true);
    }

    protected final RecursalCaseContext requireContext(RecursalCaseContext context, RecursalSpecies species) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(species, "species");
        return context;
    }

    protected final RecursalAuthority collegiateAuthorityFor(RecursalTribunal tribunal) {
        return switch (tribunal) {
            case TJ -> RecursalAuthority.CAMARA;
            case TRF, TRT, TRE, STJ, TST, TNU -> RecursalAuthority.TURMA;
            case TSE, STM, STF -> RecursalAuthority.PLENARIO;
        };
    }

    protected final RecursalRoutePlan routeReclamacaoConstitucional(RecursalCaseContext context,
                                                                    RecursalTribunalPerfilReal perfil,
                                                                    ReclamacaoConstitucional ignored) {
        RecursalTribunal destino = resolveDestinoReclamacao(context);
        InstanceLevel instanciaDestino = destino == RecursalTribunal.STF ? InstanceLevel.EXTRAORDINARY : destino.instanceLevel();
        RecursalAuthority autoridadeMerito = switch (destino) {
            case STJ -> RecursalAuthority.CORTE_ESPECIAL;
            case TSE, STM, STF -> RecursalAuthority.PLENARIO;
            default -> collegiateAuthorityFor(destino);
        };
        if (destino == context.tribunalOrigem()) {
            return sameCourtPlan(
                    perfil.perfilNome(),
                    context,
                    autoridadeMerito,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(true, RecursalAuthority.PRESIDENCIA, false, null, false, false, false, destino == RecursalTribunal.STF),
                    PreventionDisposition.strictSameRelator()
            );
        }
        return externalAutonomousPlan(
                perfil.perfilNome(),
                context,
                destino,
                instanciaDestino,
                autoridadeMerito,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, true, RecursalAuthority.PRESIDENCIA, false, false, false, destino == RecursalTribunal.STF),
                PreventionDisposition.strictSameRelator()
        );
    }

    protected final RecursalTribunal resolveDestinoReclamacao(RecursalCaseContext context) {
        if (context.tribunalOrigem() == RecursalTribunal.TSE || context.tribunalOrigem() == RecursalTribunal.TRE || context.tipoJustica() == TipoJustica.ELEITORAL) {
            return RecursalTribunal.TSE;
        }
        if (context.tribunalOrigem() == RecursalTribunal.STM || context.tipoJustica() == TipoJustica.MILITAR_FEDERAL) {
            return RecursalTribunal.STM;
        }
        if (context.tribunalOrigem() == RecursalTribunal.STF || context.materiaConstitucional()) {
            return RecursalTribunal.STF;
        }
        return RecursalTribunal.STJ;
    }

    protected final RecursalRoutePlan routeExtendedSpecies(RecursalCaseContext context, RecursalSpecies species) {
        requireContext(context, species);
        RecursalTribunalPerfilReal perfil = realProfile(context);
        return switch (species) {
            case AgravoInstrumento ignored -> {
                requireFirstInstance(context);
                yield externalAutonomousPlan(
                        perfil.perfilNome(),
                        context,
                        context.tribunalOrigem(),
                        InstanceLevel.SECOND_INSTANCE,
                        collegiateAuthorityFor(context.tribunalOrigem()),
                        ordinaryCivilPreparo(context),
                        new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                        new PreventionDisposition(true, false, true, true));
            }
            case AgravoInstrumentoTrabalhista ignored -> externalAutonomousPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.TRT,
                    InstanceLevel.SECOND_INSTANCE,
                    RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    new PreventionDisposition(true, false, true, true));
            case AgravoRegimental ignored -> sameCourtPlan(
                    perfil.perfilNome(),
                    context,
                    collegiateAuthorityFor(context.tribunalOrigem()),
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    PreventionDisposition.strictSameRelator());
            case RecursoOrdinarioConstitucional ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    context.tribunalOrigem().secondInstanceCourt() ? RecursalTribunal.STJ : RecursalTribunal.STF,
                    context.tribunalOrigem().secondInstanceCourt() ? InstanceLevel.SUPERIOR : InstanceLevel.EXTRAORDINARY,
                    context.tribunalOrigem().secondInstanceCourt() ? RecursalAuthority.TURMA : RecursalAuthority.PLENARIO,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(true, perfil.autoridadeAdmissibilidadeExcepcional(), true, RecursalAuthority.RELATOR, false, false, false, false),
                    new PreventionDisposition(true, false, true, true));
            case RecursoOrdinarioTrabalhista ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.TRT,
                    InstanceLevel.SECOND_INSTANCE,
                    RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                    new PreventionDisposition(true, false, true, true));
            case RecursoRevista ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.TST,
                    InstanceLevel.SUPERIOR,
                    RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(true, perfil.autoridadeAdmissibilidadeExcepcional(), true, RecursalAuthority.RELATOR, true, true, true, false),
                    new PreventionDisposition(true, false, true, true));
            case AgravoRecursoRevista ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.TST,
                    InstanceLevel.SUPERIOR,
                    RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, true, false, false),
                    new PreventionDisposition(true, false, true, true));
            case AgravoPeticao ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.TRT,
                    InstanceLevel.SECOND_INSTANCE,
                    RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                    new PreventionDisposition(true, false, true, true));
            case EmbargosExecucao ignored -> new RecursalRoutePlan(
                    perfil.perfilNome(),
                    context.tribunalOrigem(),
                    tribunalDetalhadoResolver.resolveOrigem(context),
                    null,
                    context.tribunalOrigem(),
                    tribunalDetalhadoResolver.resolveDestino(context, context.tribunalOrigem()),
                    context.instanciaAtual(),
                    null,
                    RecursalAuthority.JUIZO_SINGULAR,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    PreventionDisposition.strictSameRelator(),
                    RemessaDisposition.internaAutuacaoDependencia()
            );
            case EmbargosExecucaoFiscal ignored -> new RecursalRoutePlan(
                    perfil.perfilNome(),
                    context.tribunalOrigem(),
                    tribunalDetalhadoResolver.resolveOrigem(context),
                    null,
                    context.tribunalOrigem(),
                    tribunalDetalhadoResolver.resolveDestino(context, context.tribunalOrigem()),
                    context.instanciaAtual(),
                    null,
                    RecursalAuthority.JUIZO_SINGULAR,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    PreventionDisposition.strictSameRelator(),
                    RemessaDisposition.internaAutuacaoDependencia()
            );
            case EmbargosTerceiro ignored -> new RecursalRoutePlan(
                    perfil.perfilNome(),
                    context.tribunalOrigem(),
                    tribunalDetalhadoResolver.resolveOrigem(context),
                    null,
                    context.tribunalOrigem(),
                    tribunalDetalhadoResolver.resolveDestino(context, context.tribunalOrigem()),
                    context.instanciaAtual(),
                    null,
                    RecursalAuthority.JUIZO_SINGULAR,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    PreventionDisposition.strictSameRelator(),
                    RemessaDisposition.internaAutuacaoDependencia()
            );
            case ReclamacaoConstitucional reclamacao -> routeReclamacaoConstitucional(context, perfil, reclamacao);
            case ConflitoCompetencia ignored -> externalAutonomousPlan(
                    perfil.perfilNome(),
                    context,
                    context.tribunalOrigem().superiorCourt() || context.tribunalOrigem().constitutionalCourt() ? RecursalTribunal.STF : RecursalTribunal.STJ,
                    context.tribunalOrigem().superiorCourt() || context.tribunalOrigem().constitutionalCourt() ? InstanceLevel.EXTRAORDINARY : InstanceLevel.SUPERIOR,
                    context.tribunalOrigem().superiorCourt() || context.tribunalOrigem().constitutionalCourt() ? RecursalAuthority.PLENARIO : RecursalAuthority.SECAO,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, false, false, false),
                    new PreventionDisposition(false, false, false, false));
            case CorrecaoParcial ignored -> sameCourtPlan(
                    perfil.perfilNome(),
                    context,
                    collegiateAuthorityFor(context.tribunalOrigem()),
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    PreventionDisposition.strictSameRelator());
            case RecursoInominadoJuizado ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    context.tribunalOrigem(),
                    InstanceLevel.SECOND_INSTANCE,
                    RecursalAuthority.TURMA,
                    ordinaryCivilPreparo(context),
                    new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                    new PreventionDisposition(true, false, true, true));
            case PedidoUniformizacaoFederal ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.TNU,
                    InstanceLevel.SECOND_INSTANCE,
                    RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(true, RecursalAuthority.PRESIDENCIA, false, null, false, true, false, false),
                    new PreventionDisposition(true, false, true, true));
            default -> throw new IllegalArgumentException("Espécie recursal ainda não suportada para o perfil " + name() + ": " + species.formalName());
        };
    }
}
