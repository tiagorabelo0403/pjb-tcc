package com.tcc.pjb.backend.service.processual.representacao;

import com.tcc.pjb.backend.core.util.EnumText;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RepresentacaoProcessualPolicyService {

    public RepresentacaoProcessualPolicyResponse resolve(Processo processo,
                                                         Usuario ator,
                                                         String requestedInstrumentRaw,
                                                         Long audienciaId,
                                                         String tipoAudienciaRaw,
                                                         boolean contextoConsensual,
                                                         boolean poderesEspeciaisTransigir,
                                                         String termoAudienciaReferencia,
                                                         String ataAudienciaReferencia) {
        return resolve(
                processo == null || processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                processo == null || processo.getRito() == null ? null : processo.getRito().name(),
                processo == null ? null : firstNonBlank(processo.getTribunal(), processo.getUf(), processo.getComarca()),
                ator == null ? null : ator.getTipoUsuario(),
                requestedInstrumentRaw,
                audienciaId,
                tipoAudienciaRaw,
                contextoConsensual,
                poderesEspeciaisTransigir,
                termoAudienciaReferencia,
                ataAudienciaReferencia
        );
    }

    public RepresentacaoProcessualPolicyResponse resolve(String ramoRaw,
                                                         String ritoRaw,
                                                         String tribunalRaw,
                                                         TipoUsuario perfilAtor,
                                                         String requestedInstrumentRaw,
                                                         Long audienciaId,
                                                         String tipoAudienciaRaw,
                                                         boolean contextoConsensual,
                                                         boolean poderesEspeciaisTransigir,
                                                         String termoAudienciaReferencia,
                                                         String ataAudienciaReferencia) {
        RamoDireito ramo = parseRamo(ramoRaw);
        RitoProcessual rito = parseRito(ritoRaw);
        TipoAudiencia tipoAudiencia = parseTipoAudiencia(tipoAudienciaRaw);
        boolean trabalhista = ramo == RamoDireito.TRABALHISTA || (rito != null && rito.isTrabalhista());
        boolean contextoAudiencia = audienciaId != null || tipoAudiencia != null || hasText(termoAudienciaReferencia) || hasText(ataAudienciaReferencia);
        boolean consensual = contextoConsensual || isAudienciaConsensual(tipoAudiencia);
        InstrumentoRepresentacaoProcessual requested = InstrumentoRepresentacaoProcessual.fromString(requestedInstrumentRaw);
        InstrumentoRepresentacaoProcessual resolved = resolveInstrument(perfilAtor, requested, trabalhista, contextoAudiencia);
        boolean admiteApudActa = trabalhista;
        boolean admiteJusPostulandi = trabalhista && !isTst(tribunalRaw);
        boolean dispensaMandatoPorFuncaoInstitucional = resolved != null && resolved.isInstitucional();
        boolean exigeProcuracaoFormal = resolved == null || !resolved.dispensaMandatoFormal();
        boolean exigePoderesEspeciais = consensual || poderesEspeciaisTransigir || (resolved != null && resolved.exigePoderesEspeciaisTransigirPorNatureza());
        boolean exigeTermoOuAta = (resolved != null && resolved.dependeAtaOuTermo()) || consensual;
        boolean regularidade = true;
        List<String> alertas = new ArrayList<>();
        List<String> fundamentos = new ArrayList<>();
        List<String> documentosBase = new ArrayList<>();
        List<String> documentosAudiencia = new ArrayList<>();
        List<String> validacoes = new ArrayList<>();
        List<String> atosPermitidos = new ArrayList<>();

        if (resolved == InstrumentoRepresentacaoProcessual.PROCURACAO_APUD_ACTA) {
            if (!trabalhista) {
                regularidade = false;
                alertas.add("A constituição apud acta foi modelada como exceção trabalhista e não deve ser aplicada fora da moldura do art. 791, § 3º, da CLT.");
            }
            if (!contextoAudiencia) {
                regularidade = false;
                alertas.add("A procuração apud acta pressupõe audiência com registro em ata ou termo.");
            }
        }
        if (resolved == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA) {
            if (!trabalhista) {
                regularidade = false;
                alertas.add("O jus postulandi foi modelado apenas para trilhas trabalhistas.");
            }
            if (perfilAtor != null && perfilAtor.isAdvocacia()) {
                regularidade = false;
                alertas.add("O jus postulandi é regime de autorrepresentação excepcional da parte e não substitui mandato de advogado já constituído.");
            }
            if (isTst(tribunalRaw)) {
                regularidade = false;
                alertas.add("A trilha foi marcada como TST; o jus postulandi não deve seguir para recursos ao TST em razão da Súmula 425.");
            }
        }

        addFundamentos(fundamentos, resolved, consensual, trabalhista);
        addDocumentosBase(documentosBase, resolved, exigePoderesEspeciais, perfilAtor, trabalhista);
        addDocumentosAudiencia(documentosAudiencia, resolved, consensual, termoAudienciaReferencia, ataAudienciaReferencia);
        addValidacoes(validacoes, resolved, perfilAtor, exigePoderesEspeciais, contextoAudiencia, admiteJusPostulandi);
        addAtosPermitidos(atosPermitidos, resolved, perfilAtor, consensual, admiteJusPostulandi);

        if (perfilAtor == null || perfilAtor == TipoUsuario.CIDADAO) {
            alertas.add("Sem perfil institucional ou advocatício explícito, a regularidade da representação deve ser conferida antes do protocolo sensível.");
        }
        if (consensual && !exigePoderesEspeciais) {
            alertas.add("Sessões consensuais exigem conferência expressa de poderes para transigir, reconhecer e firmar ajustes.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.ADVOCACIA_PUBLICA_INSTITUCIONAL && ramo == RamoDireito.TRIBUTARIO) {
            atosPermitidos.add("Atuação fazendária, inclusive execução fiscal, embargos e negociações submetidas à governança do ente.");
        }

        Set<String> fundamentosUnique = new LinkedHashSet<>(fundamentos);
        Set<String> documentosBaseUnique = new LinkedHashSet<>(documentosBase);
        Set<String> documentosAudienciaUnique = new LinkedHashSet<>(documentosAudiencia);
        Set<String> validacoesUnique = new LinkedHashSet<>(validacoes);
        Set<String> alertasUnique = new LinkedHashSet<>(alertas);
        Set<String> atosUnique = new LinkedHashSet<>(atosPermitidos);

        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
        put(envelope, "requestedInstrument", requested == null ? null : requested.name());
        put(envelope, "resolvedInstrument", resolved == null ? null : resolved.name());
        put(envelope, "regimePostulacao", resolveRegime(resolved, perfilAtor));
        put(envelope, "perfilAtor", perfilAtor == null ? null : perfilAtor.name());
        put(envelope, "ramoDireito", ramo == null ? normalizeNullable(ramoRaw) : ramo.name());
        put(envelope, "ritoProcessual", rito == null ? normalizeNullable(ritoRaw) : rito.name());
        put(envelope, "tribunalCodigo", normalizeNullable(tribunalRaw));
        put(envelope, "contextoAudiencia", contextoAudiencia);
        put(envelope, "contextoConsensual", consensual);
        put(envelope, "admiteApudActa", admiteApudActa);
        put(envelope, "admiteJusPostulandi", admiteJusPostulandi);
        put(envelope, "exigeProcuracaoFormal", exigeProcuracaoFormal);
        put(envelope, "dispensaMandatoPorFuncaoInstitucional", dispensaMandatoPorFuncaoInstitucional);
        put(envelope, "exigePoderesEspeciaisTransigir", exigePoderesEspeciais);
        put(envelope, "exigeTermoOuAtaAudiencia", exigeTermoOuAta);
        put(envelope, "regularidadeSuficiente", regularidade);
        if (audienciaId != null) {
            envelope.put("audienciaId", audienciaId);
        }
        put(envelope, "tipoAudiencia", tipoAudiencia == null ? normalizeNullable(tipoAudienciaRaw) : tipoAudiencia.name());
        put(envelope, "termoAudienciaReferencia", normalizeNullable(termoAudienciaReferencia));
        put(envelope, "ataAudienciaReferencia", normalizeNullable(ataAudienciaReferencia));
        envelope.put("fundamentosLegais", List.copyOf(fundamentosUnique));
        envelope.put("documentosBase", List.copyOf(documentosBaseUnique));
        envelope.put("documentosAudiencia", List.copyOf(documentosAudienciaUnique));
        envelope.put("validacoesObrigatorias", List.copyOf(validacoesUnique));
        envelope.put("alertas", List.copyOf(alertasUnique));
        envelope.put("atosPermitidos", List.copyOf(atosUnique));

        return new RepresentacaoProcessualPolicyResponse(
                requested == null ? null : requested.name(),
                resolved == null ? null : resolved.name(),
                resolveRegime(resolved, perfilAtor),
                perfilAtor == null ? null : perfilAtor.name(),
                ramo == null ? normalizeNullable(ramoRaw) : ramo.name(),
                rito == null ? normalizeNullable(ritoRaw) : rito.name(),
                normalizeNullable(tribunalRaw),
                exigeProcuracaoFormal,
                admiteApudActa,
                admiteJusPostulandi,
                dispensaMandatoPorFuncaoInstitucional,
                exigePoderesEspeciais,
                exigeTermoOuAta,
                contextoAudiencia,
                consensual,
                regularidade,
                List.copyOf(fundamentosUnique),
                List.copyOf(documentosBaseUnique),
                List.copyOf(documentosAudienciaUnique),
                List.copyOf(validacoesUnique),
                List.copyOf(alertasUnique),
                List.copyOf(atosUnique),
                Map.copyOf(envelope)
        );
    }

    public boolean representacaoSuficiente(RepresentacaoProcessualPolicyResponse policy,
                                           boolean possuiDocumentoProcuracao,
                                           boolean possuiIdentificacaoAdvocaticia) {
        if (policy == null) {
            return possuiDocumentoProcuracao;
        }
        if (!policy.regularidadeSuficiente()) {
            return false;
        }
        if (!policy.exigeProcuracaoFormal()) {
            return true;
        }
        if (policy.dispensaMandatoPorFuncaoInstitucional()) {
            return true;
        }
        return possuiDocumentoProcuracao && possuiIdentificacaoAdvocaticia;
    }

    private InstrumentoRepresentacaoProcessual resolveInstrument(TipoUsuario perfilAtor,
                                                                 InstrumentoRepresentacaoProcessual requested,
                                                                 boolean trabalhista,
                                                                 boolean contextoAudiencia) {
        if (perfilAtor != null) {
            if (perfilAtor.isDefensoriaPublica()) {
                return InstrumentoRepresentacaoProcessual.DEFENSORIA_PUBLICA_INSTITUCIONAL;
            }
            if (perfilAtor.isProcuradoria()) {
                return InstrumentoRepresentacaoProcessual.ADVOCACIA_PUBLICA_INSTITUCIONAL;
            }
            if (perfilAtor.isMinisterioPublico()) {
                return InstrumentoRepresentacaoProcessual.MINISTERIO_PUBLICO_FUNCIONAL;
            }
            if (perfilAtor == TipoUsuario.CURADOR_ESPECIAL) {
                return InstrumentoRepresentacaoProcessual.CURADORIA_ESPECIAL;
            }
        }
        if (requested != null) {
            return requested;
        }
        if (perfilAtor == TipoUsuario.CIDADAO && trabalhista) {
            return InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA;
        }
        if (trabalhista && contextoAudiencia && perfilAtor != null && perfilAtor.isAdvocacia()) {
            return InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA;
        }
        return InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA;
    }

    private void addFundamentos(List<String> fundamentos,
                                InstrumentoRepresentacaoProcessual resolved,
                                boolean consensual,
                                boolean trabalhista) {
        if (resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA || resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA_ET_EXTRA) {
            fundamentos.add("CF/88, art. 133, e CPC, art. 105.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.PROCURACAO_APUD_ACTA) {
            fundamentos.add("CLT, art. 791, § 3º, incluído pela Lei 12.437/2011.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA || trabalhista) {
            fundamentos.add("CLT, art. 791, caput, com limitação recursal da Súmula 425 do TST e Res. 165/2010.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.DEFENSORIA_PUBLICA_INSTITUCIONAL) {
            fundamentos.add("CF/88, art. 134.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.ADVOCACIA_PUBLICA_INSTITUCIONAL) {
            fundamentos.add("CF/88, arts. 131 e 132.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.MINISTERIO_PUBLICO_FUNCIONAL) {
            fundamentos.add("CF/88, arts. 127 e 129.");
        }
        if (consensual) {
            fundamentos.add("Resolução CNJ 125/2010 e Lei 13.140/2015, arts. 14 e 30.");
        }
    }

    private void addDocumentosBase(List<String> documentos,
                                   InstrumentoRepresentacaoProcessual resolved,
                                   boolean exigePoderesEspeciais,
                                   TipoUsuario perfilAtor,
                                   boolean trabalhista) {
        if (resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA || resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA_ET_EXTRA) {
            documentos.add("Procuração geral para o foro com identificação do advogado e da parte.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA_ET_EXTRA || exigePoderesEspeciais) {
            documentos.add("Cláusula específica para transigir, confessar, reconhecer pedido, renunciar, receber e dar quitação.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.PREPOSICAO_COM_PODERES) {
            documentos.add("Carta de preposição ou mandato equivalente com poderes expressos para comparecimento e transação.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.DEFENSORIA_PUBLICA_INSTITUCIONAL) {
            documentos.add("Identificação funcional da Defensoria e vínculo do assistido com a atuação institucional.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.ADVOCACIA_PUBLICA_INSTITUCIONAL) {
            documentos.add("Credencial funcional ou peça institucional que demonstre a representação judicial do ente público.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.MINISTERIO_PUBLICO_FUNCIONAL) {
            documentos.add("Identificação funcional ministerial e vinculação ao feito ou órgão oficiante.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.CURADORIA_ESPECIAL) {
            documentos.add("Ato judicial de nomeação da curadoria especial ou registro equivalente no processo.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA && trabalhista) {
            documentos.add("Qualificação pessoal da parte, documentos trabalhistas essenciais e ciência das limitações da Súmula 425 do TST.");
        }
        if (perfilAtor != null && perfilAtor.isAdvocacia()) {
            documentos.add("OAB válida e higidez da cadeia de representação.");
        }
    }

    private void addDocumentosAudiencia(List<String> documentos,
                                        InstrumentoRepresentacaoProcessual resolved,
                                        boolean consensual,
                                        String termoAudienciaReferencia,
                                        String ataAudienciaReferencia) {
        if (resolved == InstrumentoRepresentacaoProcessual.PROCURACAO_APUD_ACTA || consensual) {
            documentos.add("Termo de audiência com registro da constituição ou dos poderes exercidos em sessão.");
            documentos.add("Ata de audiência ou ata de conciliação/mediação com identificação das partes e do representante.");
        }
        if (hasText(termoAudienciaReferencia)) {
            documentos.add("Referência do termo: " + termoAudienciaReferencia.trim());
        }
        if (hasText(ataAudienciaReferencia)) {
            documentos.add("Referência da ata: " + ataAudienciaReferencia.trim());
        }
    }

    private void addValidacoes(List<String> validacoes,
                               InstrumentoRepresentacaoProcessual resolved,
                               TipoUsuario perfilAtor,
                               boolean exigePoderesEspeciais,
                               boolean contextoAudiencia,
                               boolean admiteJusPostulandi) {
        if (resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA || resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA_ET_EXTRA) {
            validacoes.add("Conferir assinatura, extensão de poderes, vigência e aderência do mandato ao ato processual pretendido.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.PROCURACAO_APUD_ACTA) {
            validacoes.add("Confirmar anuência verbal da parte representada e registro expresso em ata ou termo de audiência.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA) {
            validacoes.add("Restringir a autorrepresentação às hipóteses admitidas na Justiça do Trabalho e bloquear subida indevida ao TST.");
        }
        if (resolved != null && resolved.isInstitucional()) {
            validacoes.add("Confirmar competência funcional, lotação, legitimidade institucional e ausência de extrapolação de atribuições.");
        }
        if (exigePoderesEspeciais) {
            validacoes.add("Exigir poderes especiais para transigir, reconhecer pedido, desistir, receber e dar quitação quando o ato envolver consenso.");
        }
        if (contextoAudiencia) {
            validacoes.add("Preservar o vínculo entre a representação e os documentos de audiência, inclusive termo e ata.");
        }
        if (perfilAtor == null && !admiteJusPostulandi) {
            validacoes.add("Sem perfil do ator, realizar conferência humana da capacidade postulatória antes de protocolo definitivo.");
        }
    }

    private void addAtosPermitidos(List<String> atos,
                                   InstrumentoRepresentacaoProcessual resolved,
                                   TipoUsuario perfilAtor,
                                   boolean consensual,
                                   boolean admiteJusPostulandi) {
        if (resolved == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA && admiteJusPostulandi) {
            atos.add("Reclamar pessoalmente e acompanhar a causa nos limites ordinários da Justiça do Trabalho.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.PROCURACAO_APUD_ACTA) {
            atos.add("Constituição imediata do patrono em audiência com lastro em termo ou ata.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA || resolved == InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA_ET_EXTRA) {
            atos.add("Petição inicial, defesa, recurso, embargos, manifestações e atos ordinários do processo.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.DEFENSORIA_PUBLICA_INSTITUCIONAL) {
            atos.add("Ajuizamento, defesa, recurso e tutela integral do assistido em todos os graus.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.ADVOCACIA_PUBLICA_INSTITUCIONAL) {
            atos.add("Representação judicial e extrajudicial do ente público, inclusive execução fiscal e acordos submetidos à governança.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.MINISTERIO_PUBLICO_FUNCIONAL) {
            atos.add("Parecer, promoção, requerimentos, recursos e intervenção funcional na forma constitucional.");
        }
        if (resolved == InstrumentoRepresentacaoProcessual.CURADORIA_ESPECIAL) {
            atos.add("Atuação protetiva vinculada ao ato judicial de nomeação e ao estado do processo.");
        }
        if (consensual) {
            atos.add("Participação em conciliação ou mediação com observância de confidencialidade, poderes de negociação e termo final de sessão.");
        }
        if (perfilAtor != null && perfilAtor.isAdvocacia()) {
            atos.add("Assinatura digital, protocolo, contrarrazões, embargos e atuação recursal conforme o mandato e a legislação de regência.");
        }
    }

    private String resolveRegime(InstrumentoRepresentacaoProcessual resolved, TipoUsuario perfilAtor) {
        if (resolved == null) {
            return "REPRESENTACAO_NAO_CLASSIFICADA";
        }
        if (resolved == InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA) {
            return "AUTORREPRESENTACAO_TRABALHISTA_EXCEPCIONAL";
        }
        if (resolved.isInstitucional()) {
            return "REPRESENTACAO_INSTITUCIONAL";
        }
        if (resolved == InstrumentoRepresentacaoProcessual.PROCURACAO_APUD_ACTA) {
            return "CONSTITUICAO_IMEDIATA_EM_AUDIENCIA";
        }
        if (resolved == InstrumentoRepresentacaoProcessual.PREPOSICAO_COM_PODERES) {
            return "REPRESENTACAO_NEGOCIAL_COM_PREPOSICAO";
        }
        if (perfilAtor != null && perfilAtor.isAdvocacia()) {
            return "REPRESENTACAO_ADVOCATICA_PRIVADA";
        }
        return "REPRESENTACAO_PROCESSUAL_ORDINARIA";
    }

    private RamoDireito parseRamo(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return RamoDireito.fromString(EnumText.normalizeToken(raw));
        } catch (Exception ignored) {
            return null;
        }
    }

    private RitoProcessual parseRito(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        return RitoProcessual.tryParse(raw).orElse(null);
    }

    private TipoAudiencia parseTipoAudiencia(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return TipoAudiencia.valueOf(EnumText.normalizeToken(raw));
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isAudienciaConsensual(TipoAudiencia tipo) {
        if (tipo == null) {
            return false;
        }
        TipoAudiencia normalized = tipo.normalizar();
        return normalized == TipoAudiencia.CONCILIACAO || normalized == TipoAudiencia.MEDIACAO;
    }

    private boolean isTst(String tribunalRaw) {
        String normalized = EnumText.normalizeToken(tribunalRaw);
        return Objects.equals(normalized, "TST") || normalized.contains("TRIBUNAL_SUPERIOR_DO_TRABALHO");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void put(LinkedHashMap<String, Object> map, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String s && s.isBlank()) {
            return;
        }
        map.put(key, value);
    }
}
