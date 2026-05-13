package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.core.operational.MagistraturaAccessScopeResolver;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.util.Locale;

public record MagistraturaAuthorityProjection(
        String scope,
        String authorityAxis,
        String judgmentAxis,
        String authorityLabel,
        String panelRoute,
        String processMeshRoute,
        String returnRoute,
        String orgaoLabel,
        String justiceAxis,
        String tribunalAxis,
        String authorityClass,
        String institutionalPanelCode,
        String institutionalLandingPath,
        String authorityUnitCode,
        String authorityUnitLabel,
        String authorityUnitBindingKey
) {

    public static MagistraturaAuthorityProjection resolve(Processo processo,
                                                          Usuario actor,
                                                          MagistraturaJudicialActCode action,
                                                          MagistraturaJudicialProvidenceCode providence,
                                                          MagistraturaJudicialActCommandRequest request,
                                                          SecretariatOperationalRoutingProfile routingProfile) {
        long processoId = processo == null || processo.getId() == null ? -1L : processo.getId();
        TipoUsuario tipo = actor == null ? null : actor.getTipoUsuario();
        if (tipo == null || !tipo.isMagistratura()) {
            tipo = inferAuthorityTipo(routingProfile);
        }
        String tribunalCodigo = firstNonBlank(
                processo == null ? null : processo.getTribunalCodigoRoteado(),
                routingProfile == null ? null : routingProfile.tribunalCodigo(),
                request == null ? null : request.orgao(),
                actor == null ? null : actor.getPerfil()
        );
        String authorityUnitCode = resolveAuthorityUnitCode(processo, actor, request, routingProfile);
        String authorityUnitLabel = resolveAuthorityUnitLabel(processo, actor, request, routingProfile, tipo);
        MagistraturaAccessScopeResolver.AccessScope accessScope = MagistraturaAccessScopeResolver.resolve(
                tipo,
                tribunalCodigo,
                authorityUnitCode,
                firstNonBlank(routingProfile == null ? null : routingProfile.secretariatCode(), authorityUnitLabel, actor == null ? null : actor.getPerfil())
        );
        String orgaoLabel = resolveOrgaoLabel(request, routingProfile, tipo, accessScope.tribunalAxis(), authorityUnitLabel);
        String authorityUnitBindingKey = buildAuthorityUnitBindingKey(accessScope.justiceAxis(), accessScope.tribunalAxis(), authorityUnitCode, authorityUnitLabel, routingProfile);
        String institutionalLandingPath = resolveInstitutionalLandingPath(accessScope, authorityUnitCode, routingProfile, orgaoLabel);
        if (tipo == TipoUsuario.MINISTRO) {
            String judgmentAxis = resolveSuperiorJudgmentAxis(request, routingProfile);
            String panelRoute = switch (action) {
                case DECISAO_MONOCRATICA -> OperationalApiRoutes.ministroPlenarioDecisaoMonocratica(processoId > 0 ? processoId : null);
                case INCLUSAO_PAUTA -> OperationalApiRoutes.ministroPlenarioPauta(processoId > 0 ? processoId : null);
                case DECISAO_PLENARIA, ACORDAO -> OperationalApiRoutes.ministroPlenarioDecisaoPlenaria(processoId > 0 ? processoId : null);
                default -> OperationalApiRoutes.ministroPlenarioMalhaProcesso(processoId > 0 ? processoId : null);
            };
            String returnRoute = switch (providence) {
                case PROVIDENCIAR_PUBLICACAO, REMETER_COLEGIADO_OU_PLENARIO -> OperationalApiRoutes.ministroPlenarioMalhaProcesso(processoId > 0 ? processoId : null);
                default -> panelRoute;
            };
            return new MagistraturaAuthorityProjection(
                    "SUPERIOR",
                    "MINISTRO_RELATOR",
                    judgmentAxis,
                    "Ministro",
                    panelRoute,
                    OperationalApiRoutes.ministroPlenarioMalhaProcesso(processoId > 0 ? processoId : null),
                    returnRoute,
                    orgaoLabel,
                    accessScope.justiceAxis(),
                    accessScope.tribunalAxis(),
                    accessScope.authorityClass(),
                    accessScope.panelCode(),
                    institutionalLandingPath,
                    authorityUnitCode,
                    authorityUnitLabel,
                    authorityUnitBindingKey
            );
        }
        if (tipo == TipoUsuario.DESEMBARGADOR || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            String judgmentAxis = resolveSecondInstanceJudgmentAxis(request, routingProfile);
            String panelRoute = switch (action) {
                case VOTO_COLEGIADO -> OperationalApiRoutes.desembargadorColegiadoVoto(processoId > 0 ? processoId : null);
                case ACORDAO -> OperationalApiRoutes.desembargadorColegiadoAcordao(processoId > 0 ? processoId : null);
                case PEDIDO_VISTA -> OperationalApiRoutes.desembargadorColegiadoVista(processoId > 0 ? processoId : null);
                case DESTAQUE -> OperationalApiRoutes.desembargadorColegiadoDestaque(processoId > 0 ? processoId : null);
                default -> "PLENARIO".equals(judgmentAxis)
                        ? OperationalApiRoutes.desembargadorPlenarioRelator(null)
                        : OperationalApiRoutes.desembargadorColegiadoMalhaProcesso(processoId > 0 ? processoId : null);
            };
            String processMeshRoute = "PLENARIO".equals(judgmentAxis)
                    ? OperationalApiRoutes.desembargadorPlenarioRelator(null)
                    : OperationalApiRoutes.desembargadorColegiadoMalhaProcesso(processoId > 0 ? processoId : null);
            return new MagistraturaAuthorityProjection(
                    "SEGUNDO_GRAU",
                    "DESEMBARGADOR_RELATOR",
                    judgmentAxis,
                    tipo == TipoUsuario.DESEMBARGADOR_FEDERAL ? "Desembargador Federal" : "Desembargador",
                    panelRoute,
                    processMeshRoute,
                    processMeshRoute,
                    orgaoLabel,
                    accessScope.justiceAxis(),
                    accessScope.tribunalAxis(),
                    accessScope.authorityClass(),
                    accessScope.panelCode(),
                    institutionalLandingPath,
                    authorityUnitCode,
                    authorityUnitLabel,
                    authorityUnitBindingKey
            );
        }
        String panelRoute = switch (action) {
            case DESPACHO -> OperationalApiRoutes.judgeGabineteDespacho(processoId > 0 ? processoId : null);
            case DECISAO_INTERLOCUTORIA -> OperationalApiRoutes.judgeGabineteDecisaoInterlocutoria(processoId > 0 ? processoId : null);
            case SENTENCA -> OperationalApiRoutes.judgeGabineteSentenca(processoId > 0 ? processoId : null);
            case DESIGNAR_AUDIENCIA -> OperationalApiRoutes.judgeGabineteAudiencia(processoId > 0 ? processoId : null);
            case ORDEM_CUMPRIMENTO_OFICIAL -> OperationalApiRoutes.judgeGabineteOrdemCumprimentoOficial(processoId > 0 ? processoId : null);
            case CERTIDAO_TRANSITO_JULGADO -> OperationalApiRoutes.judgeGabineteCertidaoTransitoJulgado(processoId > 0 ? processoId : null);
            default -> "/api/v1/magistratura/processos/" + (processoId > 0 ? processoId : "{processoId}") + "/atos";
        };
        return new MagistraturaAuthorityProjection(
                "PRIMEIRO_GRAU",
                "JUIZ_GABINETE",
                "MONOCRATICO",
                resolveJudgeLabel(tipo, accessScope.justiceAxis()),
                panelRoute,
                panelRoute,
                panelRoute,
                orgaoLabel,
                accessScope.justiceAxis(),
                accessScope.tribunalAxis(),
                accessScope.authorityClass(),
                accessScope.panelCode(),
                institutionalLandingPath,
                authorityUnitCode,
                authorityUnitLabel,
                authorityUnitBindingKey
        );
    }

    public static MagistraturaAuthorityProjection resolveRecursal(Processo processo,
                                                                  Usuario actor,
                                                                  SecretariatOperationalRoutingProfile routingProfile,
                                                                  String stageToken,
                                                                  String orgaoHint) {
        MagistraturaJudicialActCommandRequest synthetic = new MagistraturaJudicialActCommandRequest(
                stageToken,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                orgaoHint,
                null,
                null,
                null
        );
        MagistraturaJudicialActCode action = switch (stageToken) {
            case "ADMISSIBILIDADE", "COLEGIADO", "EMBARGOS" -> MagistraturaJudicialActCode.DESPACHO_RELATOR;
            case "PAUTA" -> MagistraturaJudicialActCode.INCLUSAO_PAUTA;
            case "ACORDAO" -> MagistraturaJudicialActCode.ACORDAO;
            default -> MagistraturaJudicialActCode.DESPACHO;
        };
        MagistraturaJudicialProvidenceCode providence = switch (stageToken) {
            case "ADMISSIBILIDADE", "COLEGIADO", "PAUTA", "ACORDAO", "EMBARGOS" -> MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO;
            default -> MagistraturaJudicialProvidenceCode.CUMPRIR_DETERMINACAO_CARTORIO;
        };
        return resolve(processo, actor, action, providence, synthetic, routingProfile);
    }

    private static String resolveInstitutionalLandingPath(MagistraturaAccessScopeResolver.AccessScope accessScope,
                                                          String authorityUnitCode,
                                                          SecretariatOperationalRoutingProfile routingProfile,
                                                          String orgaoLabel) {
        String caixaCodigo = firstNonBlank(
                routingProfile == null ? null : routingProfile.secretariatCode(),
                routingProfile == null ? null : routingProfile.deskAxis(),
                orgaoLabel
        );
        return switch (accessScope.authorityClass()) {
            case "MINISTRO" -> OperationalApiRoutes.ministroPlenarioPainel(accessScope.tribunalAxis(), authorityUnitCode, caixaCodigo, orgaoLabel);
            case "DESEMBARGADOR" -> OperationalApiRoutes.desembargadorPainel(accessScope.justiceAxis(), accessScope.tribunalAxis(), authorityUnitCode, caixaCodigo, orgaoLabel);
            default -> OperationalApiRoutes.judgeGabinetePainel(accessScope.justiceAxis(), accessScope.tribunalAxis(), authorityUnitCode, caixaCodigo, orgaoLabel);
        };
    }

    private static String buildAuthorityUnitBindingKey(String justiceAxis,
                                                       String tribunalAxis,
                                                       String authorityUnitCode,
                                                       String authorityUnitLabel,
                                                       SecretariatOperationalRoutingProfile routingProfile) {
        return normalize(firstNonBlank(justiceAxis, "ESTADUAL"))
                + '>' + normalize(firstNonBlank(tribunalAxis, "TJ"))
                + '>' + normalize(firstNonBlank(routingProfile == null ? null : routingProfile.instanciaAxis(), "PRIMEIRO_GRAU"))
                + '>' + normalize(firstNonBlank(authorityUnitCode, routingProfile == null ? null : routingProfile.secretariatCode(), "UNIDADE_BASE"))
                + '>' + normalize(firstNonBlank(authorityUnitLabel, routingProfile == null ? null : routingProfile.deskAxis(), "ORGAO"));
    }

    private static String resolveAuthorityUnitCode(Processo processo,
                                                   Usuario actor,
                                                   MagistraturaJudicialActCommandRequest request,
                                                   SecretariatOperationalRoutingProfile routingProfile) {
        boolean preferRoutingUnit = routingProfile != null
                && routingProfile.instanciaAxis() != null
                && !"PRIMEIRO_GRAU".equalsIgnoreCase(routingProfile.instanciaAxis());
        return preferRoutingUnit
                ? firstNonBlank(
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("unidadeJudiciariaCodigo")),
                routingProfile == null ? null : routingProfile.secretariatCode(),
                processo == null ? null : processo.getUnidadeJudiciariaCodigo(),
                actor == null ? null : actor.getPerfil(),
                request == null ? null : request.orgao()
        )
                : firstNonBlank(
                processo == null ? null : processo.getUnidadeJudiciariaCodigo(),
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("unidadeJudiciariaCodigo")),
                routingProfile == null ? null : routingProfile.secretariatCode(),
                actor == null ? null : actor.getPerfil(),
                request == null ? null : request.orgao()
        );
    }

    private static String resolveAuthorityUnitLabel(Processo processo,
                                                    Usuario actor,
                                                    MagistraturaJudicialActCommandRequest request,
                                                    SecretariatOperationalRoutingProfile routingProfile,
                                                    TipoUsuario tipo) {
        if (tipo == TipoUsuario.MINISTRO || tipo == TipoUsuario.DESEMBARGADOR || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return firstNonBlank(
                    request == null ? null : request.orgao(),
                    routingProfile == null ? null : routingProfile.deskAxis(),
                    actor == null ? null : actor.getPerfil(),
                    processo == null ? null : processo.getVara()
            );
        }
        return firstNonBlank(
                processo == null ? null : processo.getVara(),
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("vara")),
                actor == null ? null : actor.getPerfil(),
                request == null ? null : request.orgao(),
                routingProfile == null ? null : routingProfile.deskAxis()
        );
    }

    private static String resolveSecondInstanceJudgmentAxis(MagistraturaJudicialActCommandRequest request,
                                                            SecretariatOperationalRoutingProfile routingProfile) {
        String normalized = normalize(join(
                request == null ? null : request.orgao(),
                request == null ? null : request.tipo(),
                request == null ? null : request.observacao(),
                routingProfile == null ? null : routingProfile.deskAxis(),
                routingProfile == null ? null : routingProfile.instanciaAxis()
        ));
        if (containsAny(normalized, "ORGAO ESPECIAL", "ÓRGAO ESPECIAL", "PLENARIO", "PLENÁRIO")) {
            return "PLENARIO";
        }
        return "COLEGIADO";
    }

    private static String resolveSuperiorJudgmentAxis(MagistraturaJudicialActCommandRequest request,
                                                      SecretariatOperationalRoutingProfile routingProfile) {
        String normalized = normalize(join(
                request == null ? null : request.orgao(),
                request == null ? null : request.tipo(),
                request == null ? null : request.observacao(),
                routingProfile == null ? null : routingProfile.deskAxis(),
                routingProfile == null ? null : routingProfile.instanciaAxis()
        ));
        if (containsAny(normalized, "TURMA")) {
            return "TURMA";
        }
        return "PLENARIO";
    }

    private static TipoUsuario inferAuthorityTipo(SecretariatOperationalRoutingProfile routingProfile) {
        String normalized = normalize(join(
                routingProfile == null ? null : routingProfile.instanciaAxis(),
                routingProfile == null ? null : routingProfile.deskAxis(),
                routingProfile == null ? null : routingProfile.organizationalPath(),
                routingProfile == null ? null : routingProfile.tribunalCodigo(),
                routingProfile == null ? null : routingProfile.tipoJustica()
        ));
        if (containsAny(normalized, "SUPERIOR", "STJ", "STF", "TST", "TSE", "STM", "CNJ")) {
            return TipoUsuario.MINISTRO;
        }
        if (containsAny(normalized, "TRF", "JUSTICA_FEDERAL", "JUSTIÇA_FEDERAL")) {
            return TipoUsuario.DESEMBARGADOR_FEDERAL;
        }
        if (containsAny(normalized, "SEGUNDO_GRAU", "CAMARA", "CÂMARA", "TURMA", "COLEGIADO", "TJ", "TRE", "TRT")) {
            return TipoUsuario.DESEMBARGADOR;
        }
        if (containsAny(normalized, "FEDERAL", "SECAO_JUDICIARIA", "SUBSECAO_JUDICIARIA", "VARA_FEDERAL")) {
            return TipoUsuario.JUIZ_FEDERAL;
        }
        if (containsAny(normalized, "JUIZADO", "TURMA_RECURSAL")) {
            return TipoUsuario.JUIZ_ESPECIAL;
        }
        return TipoUsuario.JUIZ;
    }

    private static String resolveOrgaoLabel(MagistraturaJudicialActCommandRequest request,
                                            SecretariatOperationalRoutingProfile routingProfile,
                                            TipoUsuario tipo,
                                            String tribunalAxis,
                                            String authorityUnitLabel) {
        String explicit = firstNonBlank(
                request == null ? null : request.orgao(),
                authorityUnitLabel,
                routingProfile == null ? null : routingProfile.deskAxis(),
                routingProfile == null ? null : routingProfile.organizationalPath()
        );
        if (explicit != null) {
            return explicit;
        }
        if (tipo == TipoUsuario.MINISTRO) {
            return firstNonBlank(tribunalAxis, "PLENARIO");
        }
        if (tipo == TipoUsuario.DESEMBARGADOR || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return firstNonBlank(tribunalAxis, "COLEGIADO");
        }
        return firstNonBlank(authorityUnitLabel, tribunalAxis, "GABINETE");
    }

    private static String resolveJudgeLabel(TipoUsuario tipo, String justiceAxis) {
        if (tipo == TipoUsuario.JUIZ_FEDERAL || (justiceAxis != null && justiceAxis.contains("FEDERAL"))) {
            return "Juiz Federal";
        }
        if (tipo == TipoUsuario.JUIZ_ESPECIAL || (justiceAxis != null && justiceAxis.contains("ESPECIAL"))) {
            return "Juiz do Juizado";
        }
        if (tipo == TipoUsuario.JUIZ_ESTADUAL || "ESTADUAL".equals(justiceAxis)) {
            return "Juiz Estadual";
        }
        return "Juiz";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String join(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(value.trim());
            }
        }
        return sb.toString();
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank() || needles == null) {
            return false;
        }
        String normalized = normalize(haystack);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O').replace('Õ', 'O').replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
