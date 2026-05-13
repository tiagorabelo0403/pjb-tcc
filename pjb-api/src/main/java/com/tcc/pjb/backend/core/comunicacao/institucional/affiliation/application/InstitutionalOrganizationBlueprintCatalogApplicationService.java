package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.OrganizacaoExtraJudicialKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOrganizationBlueprintCatalogApplicationService {

    public List<InstitutionalOrganizationBlueprint> listar() {
        return List.of(
                forum(),
                secretariaJudiciaria(),
                centralAudiencias(),
                centralMandados(),
                promotoria(),
                nucleoDefensoria(),
                procuradoriaPublica(),
                delegacia(),
                policiaPenal(),
                unidadePrisional(),
                cejusc(),
                contadoria(),
                equipePsicossocial(),
                cartorioIntegrado(),
                conselhoTutelar(),
                orgaoTecnicoConveniado(),
                cooperacaoJudicialExterna(),
                genericoInstitucional()
        ).stream().sorted(Comparator.comparing(InstitutionalOrganizationBlueprint::codigo)).toList();
    }

    public Optional<InstitutionalOrganizationBlueprint> findByScope(InstitutionalOrganizationScope scope) {
        if (scope == null) {
            return Optional.empty();
        }
        return listar().stream().filter(item -> item.scope() == scope).findFirst();
    }

    public Optional<InstitutionalOrganizationBlueprint> resolve(InstitutionalOrganizationScope scope, DestinatarioInstitucionalKind kind) {
        if (scope != null) {
            Optional<InstitutionalOrganizationBlueprint> byScope = findByScope(scope);
            if (byScope.isPresent()) {
                return byScope;
            }
        }
        InstitutionalOrganizationScope inferred = inferScope(kind, null, null, null);
        return inferred == null ? Optional.empty() : findByScope(inferred);
    }

    public InstitutionalOrganizationScope inferScope(DestinatarioInstitucionalKind kind,
                                                     String unidadeCodigo,
                                                     String orgaoSigla,
                                                     String unidadeNome) {
        String codigo = normalize(unidadeCodigo);
        String sigla = normalize(orgaoSigla);
        String nome = normalize(unidadeNome);
        if (containsAny(codigo, sigla, nome, "CEJUSC", "CONCILIACAO", "MEDIACAO")) {
            return InstitutionalOrganizationScope.CEJUSC;
        }
        if (containsAny(codigo, sigla, nome, "MANDADO", "MANDADOS", "CUMPRIMENTO_MANDADOS")) {
            return InstitutionalOrganizationScope.CENTRAL_MANDADOS;
        }
        if (containsAny(codigo, sigla, nome, "AUDIENCIA", "AUDIENCIAS", "PAUTA", "CENTRAL_DE_PAUTA")) {
            return InstitutionalOrganizationScope.CENTRAL_AUDIENCIAS;
        }
        if (containsAny(codigo, sigla, nome, "FORUM", "VARA", "SECRETARIA", "GABINETE", "DIRETORIA_DO_FORO")) {
            return InstitutionalOrganizationScope.FORUM;
        }
        if (containsAny(codigo, sigla, nome, "NATJUS", "NUCLEO_DE_APOIO_TECNICO")) {
            return InstitutionalOrganizationScope.ORGAO_TECNICO_CONVENIADO;
        }
        if (containsAny(codigo, sigla, nome, "SOCIOEDUCATIVO", "CASE")) {
            return InstitutionalOrganizationScope.UNIDADE_PRISIONAL;
        }
        if (containsAny(codigo, sigla, nome, "JUIZO_DEPRECADO", "COOPERACAO")) {
            return InstitutionalOrganizationScope.COOPERACAO_JUDICIAL_EXTERNA;
        }
        if (kind == null) {
            return InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL;
        }
        return switch (kind) {
            case MINISTERIO_PUBLICO -> InstitutionalOrganizationScope.PROMOTORIA;
            case DEFENSORIA_PUBLICA -> InstitutionalOrganizationScope.NUCLEO_DEFENSORIA;
            case ADVOCACIA_PUBLICA, PROCURADORIA_ESTADO, PROCURADORIA_MUNICIPIO, AGU, FAZENDA_PUBLICA -> InstitutionalOrganizationScope.PROCURADORIA_PUBLICA;
            case DELEGACIA_POLICIA, DELEGACIA_POLICIA_CIVIL, DELEGACIA_POLICIA_FEDERAL -> InstitutionalOrganizationScope.DELEGACIA;
            case POLICIA_PENAL -> InstitutionalOrganizationScope.POLICIA_PENAL;
            case UNIDADE_PRISIONAL -> InstitutionalOrganizationScope.UNIDADE_PRISIONAL;
            case CONSELHO_TUTELAR -> InstitutionalOrganizationScope.CONSELHO_TUTELAR;
            case PERICIA_JUDICIAL, PERITO_JUDICIAL, ORGAO_TECNICO_CONVENIADO -> InstitutionalOrganizationScope.ORGAO_TECNICO_CONVENIADO;
            case CONTADORIA_JUDICIAL -> InstitutionalOrganizationScope.CONTADORIA;
            case EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL -> InstitutionalOrganizationScope.EQUIPE_PSICOSSOCIAL;
            case CEJUSC -> InstitutionalOrganizationScope.CEJUSC;
            case CARTORIO_EXTRAJUDICIAL -> InstitutionalOrganizationScope.CARTORIO_INTEGRADO;
            case JUIZO_DEPRECADO, ORGAO_JUDICIAL_EXTERNO -> InstitutionalOrganizationScope.COOPERACAO_JUDICIAL_EXTERNA;
        };
    }

    public Optional<InstitutionalAccessLaneBlueprint> resolveLane(InstitutionalOrganizationScope scope,
                                                                  InstitutionalAccessLaneKind laneKind,
                                                                  InstitutionalNominationRole role,
                                                                  FuncaoOperacionalInstitucional funcao,
                                                                  InstitutionalProcessProfile profile) {
        Optional<InstitutionalOrganizationBlueprint> blueprint = findByScope(scope == null ? InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL : scope);
        if (blueprint.isEmpty()) {
            return Optional.empty();
        }
        List<InstitutionalAccessLaneBlueprint> lanes = blueprint.get().lanes();
        if (laneKind != null) {
            Optional<InstitutionalAccessLaneBlueprint> lane = lanes.stream().filter(item -> item.laneKind() == laneKind).findFirst();
            if (lane.isPresent()) {
                return lane;
            }
        }
        if (role != null) {
            Optional<InstitutionalAccessLaneBlueprint> lane = lanes.stream().filter(item -> item.nominationRole() == role).findFirst();
            if (lane.isPresent()) {
                return lane;
            }
        }
        if (funcao != null) {
            Optional<InstitutionalAccessLaneBlueprint> lane = lanes.stream().filter(item -> item.funcaoOperacional() == funcao).findFirst();
            if (lane.isPresent()) {
                return lane;
            }
        }
        if (profile != null) {
            Optional<InstitutionalAccessLaneBlueprint> lane = lanes.stream().filter(item -> item.processProfile() == profile).findFirst();
            if (lane.isPresent()) {
                return lane;
            }
        }
        return lanes.stream().filter(item -> item.laneKind() == InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA).findFirst();
    }

    private InstitutionalOrganizationBlueprint forum() {
        return blueprint("FORUM", InstitutionalOrganizationScope.FORUM, "Fórum e direção de unidade judiciária", DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                OrganizacaoExtraJudicialKind.COOPERACAO_JUDICIAL_EXTERNA, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.DIRETORIA, "FORUM_DIRETORIA", "Diretoria do fórum", InstitutionalNominationRole.DIRETORIA_FORUM, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.DIRETOR_FORUM, InstitutionalEntryLandingPanel.PAINEL_DIRETORIA_FORUM, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Sem poder decisório jurisdicional.", "Uso do certificado fora da rede exige autorização remota."),
                                List.of("Administra triagem, lotação, cobertura e integridade institucional.")),
                        lane(InstitutionalAccessLaneKind.SECRETARIA, "FORUM_SECRETARIA", "Secretaria judiciária", InstitutionalNominationRole.SECRETARIA_FORUM, FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                                InstitutionalProcessProfile.SECRETARIA_FORUM, InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA, CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Sem assinatura de decisão.", "Atos finais dependem do fluxo jurisdicional próprio."),
                                List.of("Organiza a unidade, certifica atos internos e prepara o gabinete.")),
                        lane(InstitutionalAccessLaneKind.TRIAGEM, "FORUM_TRIAGEM", "Triagem institucional", InstitutionalNominationRole.TRIAGEM_ORGAO, FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                                InstitutionalProcessProfile.SERVIDOR_TRIAGEM, InstitutionalEntryLandingPanel.PAINEL_TRIAGEM, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem assinatura final.", "Sem redistribuição estrutural fora da unidade."),
                                List.of("Recepção, classificação e encaminhamento para secretaria e gabinete.")),
                        lane(InstitutionalAccessLaneKind.ASSESSORIA, "FORUM_ASSESSORIA", "Assessoria institucional do fórum", InstitutionalNominationRole.ASSESSORIA_INSTITUCIONAL, FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL,
                                InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_CAIXA, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                false, false, false, false,
                                List.of("Sem poder decisório próprio."),
                                List.of("Fluxo mastigado para magistrado e secretaria."))
                ),
                List.of("Adesão nasce na direção ou administração homologada.", "A unidade nomeia pessoas e o PJB ativa apenas o contexto delegado."));
    }

    private InstitutionalOrganizationBlueprint secretariaJudiciaria() {
        return blueprint("SECRETARIA_UNIDADE", InstitutionalOrganizationScope.SECRETARIA_UNIDADE_JUDICIARIA, "Secretaria de unidade judiciária", DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                OrganizacaoExtraJudicialKind.COOPERACAO_JUDICIAL_EXTERNA, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.SECRETARIA, "SECRETARIA_JUDICIARIA", "Secretaria da unidade", InstitutionalNominationRole.SECRETARIA_FORUM, FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                                InstitutionalProcessProfile.SECRETARIA_FORUM, InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA, CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Sem poder decisório."),
                                List.of("Atua sobre expedientes internos, mandados, filas e certificações.")),
                        lane(InstitutionalAccessLaneKind.TRIAGEM, "SECRETARIA_TRIAGEM", "Triagem da unidade", InstitutionalNominationRole.TRIAGEM_ORGAO, FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                                InstitutionalProcessProfile.SERVIDOR_TRIAGEM, InstitutionalEntryLandingPanel.PAINEL_TRIAGEM, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem acesso a assinatura."),
                                List.of("Operação inicial e qualificação do expediente."))
                ),
                List.of("Escopo específico para secretaria sem confundir com magistratura."));
    }

    private InstitutionalOrganizationBlueprint centralAudiencias() {
        return blueprint("CENTRAL_AUDIENCIAS", InstitutionalOrganizationScope.CENTRAL_AUDIENCIAS, "Central de audiências", DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                OrganizacaoExtraJudicialKind.COOPERACAO_JUDICIAL_EXTERNA, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                false, false, true, false,
                List.of(
                        lane(InstitutionalAccessLaneKind.AGENDAMENTO_AUDIENCIA, "AGENDA_AUDIENCIA", "Agendamento de audiência", InstitutionalNominationRole.AGENDADOR_AUDIENCIA, FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                                InstitutionalProcessProfile.AGENDADOR_AUDIENCIA, InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem decisão sobre mérito.", "Sem assinatura final institucional."),
                                List.of("Opera pauta, salas, remarcações, presenças e janelas processuais.")),
                        lane(InstitutionalAccessLaneKind.SECRETARIA, "CENTRAL_AUDIENCIA_SECRETARIA", "Secretaria da pauta", InstitutionalNominationRole.SECRETARIA_FORUM, FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                                InstitutionalProcessProfile.SECRETARIA_FORUM, InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA, CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem assinatura de atos finais."),
                                List.of("Suporte de pauta, confirmação de comparecimento, atas e presença institucional."))
                ),
                List.of("Escopo voltado a agenda e pauta institucional."));
    }

    private InstitutionalOrganizationBlueprint centralMandados() {
        return blueprint("CENTRAL_MANDADOS", InstitutionalOrganizationScope.CENTRAL_MANDADOS, "Central de mandados", DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                OrganizacaoExtraJudicialKind.COOPERACAO_JUDICIAL_EXTERNA, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.CENTRAL_MANDADOS, "CENTRAL_MANDADOS_OPERACAO", "Operação da central de mandados", InstitutionalNominationRole.SECRETARIA_FORUM, FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                                InstitutionalProcessProfile.TECNICO_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_UNIDADE, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA, CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Sem prolação de decisão."),
                                List.of("Distribui, acompanha e certifica execução de mandados dentro do fluxo autorizado.")),
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "CENTRAL_MANDADOS_GESTAO", "Gestão da central de mandados", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.COORDENADOR_UNIDADE, InstitutionalEntryLandingPanel.PAINEL_ADMINISTRATIVO, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Sem poderes jurisdicionais."),
                                List.of("Controla escala, cobertura e integridade institucional da central."))
                ),
                List.of("Fluxo especializado de diligências e certidões."));
    }

    private InstitutionalOrganizationBlueprint promotoria() {
        return essentialJusticeBlueprint("PROMOTORIA", InstitutionalOrganizationScope.PROMOTORIA, "Promotoria e Ministério Público", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                InstitutionalProcessProfile.PROMOTOR, List.of("Vista obrigatória, manifestação e gestão de caixa institucional do MP."));
    }

    private InstitutionalOrganizationBlueprint nucleoDefensoria() {
        return essentialJusticeBlueprint("NUCLEO_DEFENSORIA", InstitutionalOrganizationScope.NUCLEO_DEFENSORIA, "Núcleo da Defensoria Pública", DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                InstitutionalProcessProfile.DEFENSOR, List.of("Recepção de citações, intimações e atuação defensorial organizada por núcleo."));
    }

    private InstitutionalOrganizationBlueprint procuradoriaPublica() {
        return essentialJusticeBlueprint("PROCURADORIA_PUBLICA", InstitutionalOrganizationScope.PROCURADORIA_PUBLICA, "Procuradoria e advocacia pública", DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA,
                InstitutionalProcessProfile.PROCURADOR, List.of("Atuação institucional por procuradoria setorial, estadual, municipal ou federal."));
    }

    private InstitutionalOrganizationBlueprint delegacia() {
        return blueprint("DELEGACIA", InstitutionalOrganizationScope.DELEGACIA, "Delegacia e polícia judiciária", DestinatarioInstitucionalKind.DELEGACIA_POLICIA,
                OrganizacaoExtraJudicialKind.SEGURANCA_PUBLICA, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "DELEGACIA_GESTAO", "Gestão da delegacia", InstitutionalNominationRole.GESTOR_DELEGACIA, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.GESTOR_DELEGACIA, InstitutionalEntryLandingPanel.PAINEL_DELEGACIA, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO),
                                true, true, true, true,
                                List.of("Sem poder jurisdicional."),
                                List.of("Controla caixa institucional, escalas e distribuição interna.")),
                        lane(InstitutionalAccessLaneKind.CARTORIO_POLICIAL, "DELEGACIA_CARTORIO", "Cartório policial", InstitutionalNominationRole.SECRETARIA_FORUM, FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                                InstitutionalProcessProfile.TECNICO_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_DELEGACIA, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA),
                                true, true, true, true,
                                List.of("Sem peticionamento em nome do membro titular fora do fluxo próprio."),
                                List.of("Recebe ofícios, requisições e confirmações de diligência.")),
                        lane(InstitutionalAccessLaneKind.TITULAR, "DELEGACIA_TITULAR", "Delegado titular", InstitutionalNominationRole.TITULAR_INSTITUCIONAL, FuncaoOperacionalInstitucional.MEMBRO_TITULAR,
                                InstitutionalProcessProfile.DELEGADO, InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO),
                                true, true, true, true,
                                List.of("Atos limitados ao escopo legal da polícia judiciária."),
                                List.of("Titular responde por manifestações e confirmações que exijam autoridade policial.")),
                        lane(InstitutionalAccessLaneKind.ASSESSORIA, "DELEGACIA_ASSESSORIA", "Assessoria e apoio da delegacia", InstitutionalNominationRole.ASSESSORIA_INSTITUCIONAL, FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL,
                                InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_CAIXA, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.PREPARAR_MINUTA),
                                false, false, false, false,
                                List.of("Sem assinatura final."),
                                List.of("Preparação técnica e apoio ao delegado titular."))
                ),
                List.of("A instituição adere e a chefia nomeia seus operadores, sempre com identidade pessoal e homologação."));
    }

    private InstitutionalOrganizationBlueprint policiaPenal() {
        return blueprint("POLICIA_PENAL", InstitutionalOrganizationScope.POLICIA_PENAL, "Polícia Penal", DestinatarioInstitucionalKind.POLICIA_PENAL,
                OrganizacaoExtraJudicialKind.POLICIA_PENAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "POLICIA_PENAL_GESTAO", "Gestão da Polícia Penal", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.POLICIAL_PENAL, InstitutionalEntryLandingPanel.PAINEL_CUSTODIA_PRISIONAL, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE),
                                true, true, true, true,
                                List.of("Sem atuação jurisdicional."),
                                List.of("Coordena fluxos de custódia, escolta e apresentação.")),
                        lane(InstitutionalAccessLaneKind.CUSTODIA, "POLICIA_PENAL_CUSTODIA", "Operador de custódia", InstitutionalNominationRole.TRIAGEM_ORGAO, FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                                InstitutionalProcessProfile.POLICIAL_PENAL, InstitutionalEntryLandingPanel.PAINEL_CUSTODIA_PRISIONAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA),
                                true, true, true, true,
                                List.of("Sem manifestação jurídica final."),
                                List.of("Confirma custódia, apresentação e recebimento de ordens."))
                ),
                List.of("Escopo próprio para cadeia de custódia, escolta e apresentação do custodiado."));
    }

    private InstitutionalOrganizationBlueprint unidadePrisional() {
        return blueprint("UNIDADE_PRISIONAL", InstitutionalOrganizationScope.UNIDADE_PRISIONAL, "Unidade prisional", DestinatarioInstitucionalKind.UNIDADE_PRISIONAL,
                OrganizacaoExtraJudicialKind.ADMINISTRACAO_PRISIONAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.DIRECAO_PRISIONAL, "UNIDADE_PRISIONAL_DIRECAO", "Direção da unidade prisional", InstitutionalNominationRole.DIRETOR_UNIDADE_PRISIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL, InstitutionalEntryLandingPanel.PAINEL_CUSTODIA_PRISIONAL, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO),
                                true, true, true, true,
                                List.of("Sem assinatura de manifestação jurídica externa além do escopo administrativo."),
                                List.of("Direção confirma cumprimento, movimentação e custódia institucional.")),
                        lane(InstitutionalAccessLaneKind.CUSTODIA, "UNIDADE_PRISIONAL_CUSTODIA", "Custódia e apresentação", InstitutionalNominationRole.TRIAGEM_ORGAO, FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                                InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL, InstitutionalEntryLandingPanel.PAINEL_CUSTODIA_PRISIONAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA),
                                true, true, true, true,
                                List.of("Sem assinatura final fora da cadeia autorizada."),
                                List.of("Recebe apresentação, audiência, alvará e confirmações operacionais."))
                ),
                List.of("A unidade não entra por conta solta; entra por adesão homologada, direção e operadores nomeados."));
    }

    private InstitutionalOrganizationBlueprint cejusc() {
        return blueprint("CEJUSC", InstitutionalOrganizationScope.CEJUSC, "CEJUSC e autocomposição", DestinatarioInstitucionalKind.CEJUSC,
                OrganizacaoExtraJudicialKind.AUTOCOMPOSICAO, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                false, false, true, false,
                List.of(
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "CEJUSC_GESTAO", "Gestão do CEJUSC", InstitutionalNominationRole.GESTOR_CEJUSC, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.COORDENADOR_UNIDADE, InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE),
                                true, true, false, false,
                                List.of("Sem decisão de mérito."),
                                List.of("Gestão de pauta, conciliadores e redistribuição interna.")),
                        lane(InstitutionalAccessLaneKind.AGENDAMENTO_CONCILIACAO, "CEJUSC_AGENDAMENTO", "Agendamento de conciliação", InstitutionalNominationRole.AGENDADOR_CONCILIACAO, FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                                InstitutionalProcessProfile.AGENDADOR_CONCILIACAO, InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem homologação judicial autônoma."),
                                List.of("Gestão de sessões, pauta e comunicação com partes e conciliadores.")),
                        lane(InstitutionalAccessLaneKind.ASSESSORIA, "CEJUSC_APOIO", "Apoio técnico do CEJUSC", InstitutionalNominationRole.APOIO_TECNICO, FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL,
                                InstitutionalProcessProfile.CONCILIADOR, InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem assinatura institucional final."),
                                List.of("Execução da sessão, termos preliminares e apoio operacional."))
                ),
                List.of("Escopo específico de autocomposição, pauta e registro de sessões."));
    }

    private InstitutionalOrganizationBlueprint contadoria() {
        return blueprint("CONTADORIA", InstitutionalOrganizationScope.CONTADORIA, "Contadoria judicial", DestinatarioInstitucionalKind.CONTADORIA_JUDICIAL,
                OrganizacaoExtraJudicialKind.APOIO_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.CONTADORIA, "CONTADORIA_OPERACAO", "Operação da contadoria", InstitutionalNominationRole.GESTOR_CONTADORIA, FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL,
                                InstitutionalProcessProfile.CONTADOR_JUDICIAL, InstitutionalEntryLandingPanel.PAINEL_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA),
                                true, true, true, true,
                                List.of("Sem atuação decisória."),
                                List.of("Cálculos, memoriais, retorno técnico e certidões.")),
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "CONTADORIA_GESTAO", "Gestão da contadoria", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.COORDENADOR_UNIDADE, InstitutionalEntryLandingPanel.PAINEL_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE),
                                true, true, true, true,
                                List.of("Sem assinatura jurisdicional."),
                                List.of("Controla filas técnicas e cobertura interna."))
                ),
                List.of("Apoio técnico judicial com cadeia forte de autoria."));
    }

    private InstitutionalOrganizationBlueprint equipePsicossocial() {
        return blueprint("EQUIPE_PSICOSSOCIAL", InstitutionalOrganizationScope.EQUIPE_PSICOSSOCIAL, "Equipe psicossocial", DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL,
                OrganizacaoExtraJudicialKind.APOIO_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.PSICOSSOCIAL, "PSICOSSOCIAL_OPERACAO", "Operação psicossocial", InstitutionalNominationRole.GESTOR_PSICOSSOCIAL, FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL,
                                InstitutionalProcessProfile.PSICOLOGO_JUDICIAL, InstitutionalEntryLandingPanel.PAINEL_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA),
                                true, true, true, true,
                                List.of("Sem decisão de mérito."),
                                List.of("Laudos, estudos técnicos e devolução psicossocial.")),
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "PSICOSSOCIAL_GESTAO", "Gestão psicossocial", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.COORDENADOR_UNIDADE, InstitutionalEntryLandingPanel.PAINEL_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE),
                                true, true, true, true,
                                List.of("Sem função jurisdicional."),
                                List.of("Escala interna, cobertura e segregação de equipe técnica."))
                ),
                List.of("Escopo para psicologia, serviço social e apoio especializado."));
    }

    private InstitutionalOrganizationBlueprint cartorioIntegrado() {
        return blueprint("CARTORIO_INTEGRADO", InstitutionalOrganizationScope.CARTORIO_INTEGRADO, "Cartório integrado", DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL,
                OrganizacaoExtraJudicialKind.CARTORIO_EXTRAJUDICIAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "CARTORIO_GESTAO", "Gestão cartorária integrada", InstitutionalNominationRole.GESTOR_CARTORIO_INTEGRADO, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.CARTORIO_EXTRAJUDICIAL, InstitutionalEntryLandingPanel.PAINEL_ORGAO, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE),
                                true, true, true, true,
                                List.of("Sem atividade jurisdicional."),
                                List.of("Gestão da unidade e autenticação institucional de atos integrados.")),
                        lane(InstitutionalAccessLaneKind.ATENDIMENTO_INSTITUCIONAL, "CARTORIO_ATENDIMENTO", "Atendimento cartorário", InstitutionalNominationRole.APOIO_TECNICO, FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL,
                                InstitutionalProcessProfile.CARTORIO_EXTRAJUDICIAL, InstitutionalEntryLandingPanel.PAINEL_UNIDADE, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA),
                                true, true, true, true,
                                List.of("Sem atuação além do convênio autorizado."),
                                List.of("Atendimento, resposta e cumprimento de cooperação cartorária."))
                ),
                List.of("Convênio, resposta técnica e cadeia auditável de autoria."));
    }

    private InstitutionalOrganizationBlueprint conselhoTutelar() {
        return blueprint("CONSELHO_TUTELAR", InstitutionalOrganizationScope.CONSELHO_TUTELAR, "Conselho Tutelar", DestinatarioInstitucionalKind.CONSELHO_TUTELAR,
                OrganizacaoExtraJudicialKind.PROTECAO_INFANTOJUVENIL, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                false, false, true, false,
                List.of(
                        lane(InstitutionalAccessLaneKind.ATENDIMENTO_INSTITUCIONAL, "CONSELHO_ATENDIMENTO", "Atendimento tutelar", InstitutionalNominationRole.APOIO_TECNICO, FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL,
                                InstitutionalProcessProfile.TECNICO_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_UNIDADE, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem manifestação final fora da competência legal."),
                                List.of("Recebimento e devolução de informações tutelares.")),
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "CONSELHO_GESTAO", "Gestão do conselho", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.COORDENADOR_UNIDADE, InstitutionalEntryLandingPanel.PAINEL_ADMINISTRATIVO, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO),
                                true, true, false, false,
                                List.of("Sem poder jurisdicional."),
                                List.of("Gestão da escala e dos responsáveis tutelares."))
                ),
                List.of("Escopo protegido para fluxos infantojuvenis e tutelares."));
    }

    private InstitutionalOrganizationBlueprint orgaoTecnicoConveniado() {
        return blueprint("ORGAO_TECNICO_CONVENIADO", InstitutionalOrganizationScope.ORGAO_TECNICO_CONVENIADO, "Órgão técnico conveniado", DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO,
                OrganizacaoExtraJudicialKind.ORGAO_TECNICO_CONVENIADO, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.APOIO_TECNICO, "TEC_CONVENIADO_OPERACAO", "Operação técnica conveniada", InstitutionalNominationRole.APOIO_TECNICO, FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL,
                                InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO, InstitutionalEntryLandingPanel.PAINEL_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA),
                                true, true, true, true,
                                List.of("Sem extrapolar o convênio técnico."),
                                List.of("Recebe requisições, laudos e respostas conveniadas.")),
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "TEC_CONVENIADO_GESTAO", "Gestão conveniada", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.COORDENADOR_UNIDADE, InstitutionalEntryLandingPanel.PAINEL_TECNICO_JUDICIAL, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO),
                                true, true, true, true,
                                List.of("Sem função jurisdicional."),
                                List.of("Garante segregação, vigência e cobertura técnica."))
                ),
                List.of("Base de convênio, rastreabilidade e cadeia técnica forte."));
    }

    private InstitutionalOrganizationBlueprint cooperacaoJudicialExterna() {
        return blueprint("COOPERACAO_JUDICIAL_EXTERNA", InstitutionalOrganizationScope.COOPERACAO_JUDICIAL_EXTERNA, "Cooperação judicial externa", DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                OrganizacaoExtraJudicialKind.COOPERACAO_JUDICIAL_EXTERNA, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.COOPERACAO, "COOPERACAO_JUDICIAL_OPERACAO", "Operação de cooperação judicial", InstitutionalNominationRole.APOIO_TECNICO, FuncaoOperacionalInstitucional.GESTOR_CAIXA,
                                InstitutionalProcessProfile.COOPERACAO_JUDICIAL, InstitutionalEntryLandingPanel.PAINEL_COOPERACAO_JUDICIAL, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA, CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Sem decisão de mérito fora do juízo competente."),
                                List.of("Roteia cartas, cooperação, devolução e certificação de cumprimento.")),
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "COOPERACAO_JUDICIAL_GESTAO", "Gestão de cooperação", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.MAGISTRADO_COOPERANTE, InstitutionalEntryLandingPanel.PAINEL_COOPERACAO_JUDICIAL, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE),
                                true, true, true, true,
                                List.of("Sem deslocar competência decisória."),
                                List.of("Escopo cooperativo e gestão de fluxo entre órgãos judiciais."))
                ),
                List.of("Entrada por adesão do órgão externo e operadores nomeados."));
    }

    private InstitutionalOrganizationBlueprint genericoInstitucional() {
        return blueprint("GENERICO_INSTITUCIONAL", InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL, "Blueprint institucional genérico", DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO,
                OrganizacaoExtraJudicialKind.OUTRO, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, "GENERICO_ADMIN", "Administrador institucional", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_ADMINISTRATIVO, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO),
                                true, true, true, true,
                                List.of("Sem poderes materiais além do que a nomeação permitir."),
                                List.of("Administração, vigência, homologação e cobertura institucional.")),
                        lane(InstitutionalAccessLaneKind.ASSESSORIA, "GENERICO_ASSESSORIA", "Assessoria institucional", InstitutionalNominationRole.ASSESSORIA_INSTITUCIONAL, FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL,
                                InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_CAIXA, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.PREPARAR_MINUTA),
                                false, false, false, false,
                                List.of("Sem assinatura final."),
                                List.of("Suporte operacional e preparação de minutas.")),
                        lane(InstitutionalAccessLaneKind.TITULAR, "GENERICO_TITULAR", "Titular institucional", InstitutionalNominationRole.TITULAR_INSTITUCIONAL, FuncaoOperacionalInstitucional.MEMBRO_TITULAR,
                                InstitutionalProcessProfile.PERFIL_HIBRIDO, InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO),
                                true, true, true, true,
                                List.of("Depende do escopo jurídico do órgão."),
                                List.of("Titular institucional genérico para blocos ainda não especializados."))
                ),
                List.of("Fallback estruturado para novos órgãos e escopos."));
    }

    private InstitutionalOrganizationBlueprint essentialJusticeBlueprint(String codigo,
                                                                         InstitutionalOrganizationScope scope,
                                                                         String nome,
                                                                         DestinatarioInstitucionalKind destinatario,
                                                                         InstitutionalProcessProfile titularProfile,
                                                                         List<String> fundamentos) {
        return blueprint(codigo, scope, nome, destinatario, destinatario.toOrganizacaoExtraJudicialKind(), InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                true, true, true, true,
                List.of(
                        lane(InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA, codigo + "_GESTAO", "Gestão institucional", InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL, FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE,
                                InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_ORGAO, InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Sem substituir a manifestação jurídica do titular."),
                                List.of("Governança interna, vínculos, escalas, cobertura e autorização operacional de pauta.")),
                        lane(InstitutionalAccessLaneKind.TRIAGEM, codigo + "_TRIAGEM", "Triagem institucional", InstitutionalNominationRole.TRIAGEM_ORGAO, FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM,
                                InstitutionalProcessProfile.SERVIDOR_TRIAGEM, InstitutionalEntryLandingPanel.PAINEL_TRIAGEM, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem assinatura final."),
                                List.of("Recebe, organiza e encaminha a caixa institucional.")),
                        lane(InstitutionalAccessLaneKind.ASSESSORIA, codigo + "_ASSESSORIA", "Assessoria institucional", InstitutionalNominationRole.ASSESSORIA_INSTITUCIONAL, FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL,
                                InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_CAIXA, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                                        CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                false, false, false, false,
                                List.of("Sem poder de assinatura final por padrão."),
                                List.of("Elaboração técnica, preparação de pareceres e apoio ao titular.")),
                        lane(InstitutionalAccessLaneKind.ATENDIMENTO_INSTITUCIONAL, codigo + "_DOCUMENTOS", "Organização documental e pauta preparatória", InstitutionalNominationRole.APOIO_TECNICO, FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL,
                                InstitutionalProcessProfile.TECNICO_INSTITUCIONAL, InstitutionalEntryLandingPanel.PAINEL_CAIXA, InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                                        CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA),
                                false, false, false, false,
                                List.of("Sem assinatura final.", "Sem agendamento autônomo de audiência.", "Sem parecer final do órgão."),
                                List.of("Perfil de apoio documental organiza anexos, dossiês, atas pendentes e pedidos de audiência sem invadir a função do titular ou da secretaria.")),
                        lane(InstitutionalAccessLaneKind.TITULAR, codigo + "_TITULAR", "Titular institucional", InstitutionalNominationRole.TITULAR_INSTITUCIONAL, FuncaoOperacionalInstitucional.MEMBRO_TITULAR,
                                titularProfile, InstitutionalEntryLandingPanel.PAINEL_TITULAR, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA, CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO,
                                        CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA, CapacidadeCaixaInstitucional.EMITIR_PARECER,
                                        CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL),
                                true, true, true, true,
                                List.of("Fora da rede institucional, certificado depende de autorização remota quando exigido pela afiliação."),
                                List.of("Representação final do órgão, com identidade pessoal, vínculo homologado, parecer final e pedido de audiência rastreável."))
                ),
                fundamentos);
    }

    private InstitutionalOrganizationBlueprint blueprint(String codigo,
                                                         InstitutionalOrganizationScope scope,
                                                         String nome,
                                                         DestinatarioInstitucionalKind destinatarioKind,
                                                         OrganizacaoExtraJudicialKind organizacaoKind,
                                                         InstitutionalTrustLevel trustFloor,
                                                         boolean requerCertificado,
                                                         boolean restringeRede,
                                                         boolean permiteRemoto,
                                                         boolean requerDuplaAprovacaoAdministrador,
                                                         List<InstitutionalAccessLaneBlueprint> lanes,
                                                         List<String> fundamentos) {
        return new InstitutionalOrganizationBlueprint(
                codigo,
                scope,
                nome,
                destinatarioKind,
                organizacaoKind,
                InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                trustFloor,
                requerCertificado,
                restringeRede,
                permiteRemoto,
                requerDuplaAprovacaoAdministrador,
                lanes,
                fundamentos
        );
    }

    private InstitutionalAccessLaneBlueprint lane(InstitutionalAccessLaneKind laneKind,
                                                  String codigo,
                                                  String nome,
                                                  InstitutionalNominationRole role,
                                                  FuncaoOperacionalInstitucional funcao,
                                                  InstitutionalProcessProfile profile,
                                                  InstitutionalEntryLandingPanel panel,
                                                  InstitutionalTrustLevel trust,
                                                  java.util.Set<CapacidadeCaixaInstitucional> capacidades,
                                                  boolean requerStepUp,
                                                  boolean requerCertificado,
                                                  boolean requerRede,
                                                  boolean permiteRemoto,
                                                  List<String> restricoes,
                                                  List<String> fundamentos) {
        return new InstitutionalAccessLaneBlueprint(laneKind, codigo, nome, role, funcao, profile, panel, trust, capacidades, requerStepUp, requerCertificado, requerRede, permiteRemoto, restricoes, fundamentos);
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String a, String b, String c, String... needles) {
        ArrayList<String> values = new ArrayList<>();
        values.add(a);
        values.add(b);
        values.add(c);
        for (String value : values) {
            String token = value == null ? "" : value;
            for (String needle : needles) {
                if (token.contains(Objects.requireNonNull(needle))) {
                    return true;
                }
            }
        }
        return false;
    }
}
