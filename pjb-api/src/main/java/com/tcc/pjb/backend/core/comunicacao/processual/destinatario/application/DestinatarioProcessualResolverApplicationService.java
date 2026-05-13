package com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.DestinatarioProcessual;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualRequest;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualResult;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TrilhoComunicacaoProcessual;

@Service
public class DestinatarioProcessualResolverApplicationService {

    public ResolucaoDestinatarioProcessualResult resolver(ResolucaoDestinatarioProcessualRequest request) {
        Objects.requireNonNull(request);
        List<String> justificativas = new ArrayList<>();
        DestinatarioInstitucionalKind institucionalKind = resolveInstitutionalKind(request, justificativas);
        DestinatarioProcessualKind processualKind = resolveProcessualKind(request, institucionalKind, justificativas);
        PapelProcessualInstitucional papel = institucionalKind == null
                ? null
                : request.papelProcessualInstitucional() != null
                ? request.papelProcessualInstitucional()
                : defaultPapel(institucionalKind);
        TrilhoComunicacaoProcessual trilho = resolveTrack(processualKind, request, institucionalKind, justificativas);
        boolean exigeCaixaInstitucional = trilho == TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA;
        boolean admiteCitacao = request.citacao() != null
                ? request.citacao()
                : request.tipoComunicacao() == null || request.tipoComunicacao().isCitacao();
        boolean admiteIntimacao = request.intimacao() != null
                ? request.intimacao()
                : request.tipoComunicacao() == null || request.tipoComunicacao().isIntimacao();
        boolean exigeIntimacaoPessoal = Boolean.TRUE.equals(request.intimacaoPessoalInstitucional())
                || papel != null && papel.exigeCienciaPessoalPreferencial();
        String nomeExibicao = resolveDisplayName(request, processualKind, institucionalKind);
        String documento = normalizeDocument(request.documento(), processualKind, institucionalKind, request.unidadeInstitucionalCodigo());
        if (exigeCaixaInstitucional) {
            justificativas.add("trilho institucional ativado por destinatário órgão/unidade");
        } else if (trilho == TrilhoComunicacaoProcessual.REPRESENTACAO_PROCESSUAL) {
            justificativas.add("trilho representacional ativado por advogado/representação");
        } else {
            justificativas.add("trilho pessoal direto ativado");
        }
        if (institucionalKind != null) {
            justificativas.add("destinatário institucional normalizado para " + institucionalKind.name());
        }
        DestinatarioProcessual destinatario = new DestinatarioProcessual(
                processualKind,
                trilho,
                request.legacyKind(),
                documento,
                nomeExibicao,
                trimToNull(request.email()),
                trimToNull(request.telefone()),
                trimToNull(request.oabNumero()),
                trimToNull(request.govbrAccountId()),
                upper(request.uf()),
                trimToNull(request.comarca()),
                trimToNull(request.foro()),
                institucionalKind,
                papel,
                trimToNull(request.unidadeInstitucionalCodigo()),
                exigeCaixaInstitucional,
                exigeIntimacaoPessoal,
                admiteCitacao,
                admiteIntimacao,
                justificativas,
                null
        );
        return new ResolucaoDestinatarioProcessualResult(
                destinatario,
                trilho,
                trilho != TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA,
                trilho == TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA,
                admiteCitacao,
                admiteIntimacao,
                justificativas,
                destinatario.hashResolucao()
        );
    }

    private DestinatarioInstitucionalKind resolveInstitutionalKind(ResolucaoDestinatarioProcessualRequest request,
                                                                   List<String> justificativas) {
        if (request.destinatarioInstitucionalKind() != null) {
            justificativas.add("destinatário institucional explícito no comando");
            return request.destinatarioInstitucionalKind();
        }
        if (request.legacyKind() == null) {
            return null;
        }
        if (request.legacyKind() == NationalCommunicationRecipientKind.PESSOA_JURIDICA && Boolean.TRUE.equals(request.fazendaPublica())) {
            justificativas.add("ente fazendário inferido a partir de pessoa jurídica pública");
            return DestinatarioInstitucionalKind.FAZENDA_PUBLICA;
        }
        return DestinatarioInstitucionalKind.fromNationalCommunicationRecipientKind(request.legacyKind())
                .map(kind -> {
                    justificativas.add("destinatário institucional inferido do tipo legado");
                    return kind;
                })
                .orElse(null);
    }

    private DestinatarioProcessualKind resolveProcessualKind(ResolucaoDestinatarioProcessualRequest request,
                                                             DestinatarioInstitucionalKind institucionalKind,
                                                             List<String> justificativas) {
        if (request.destinatarioProcessualKind() != null) {
            justificativas.add("tipo processual explícito no comando");
            return request.destinatarioProcessualKind();
        }
        if (institucionalKind != null) {
            if (request.unidadeInstitucionalCodigo() != null && !request.unidadeInstitucionalCodigo().isBlank()) {
                justificativas.add("unidade institucional explícita detectada");
                return DestinatarioProcessualKind.UNIDADE_INSTITUCIONAL;
            }
            return DestinatarioProcessualKind.ORGAO_INSTITUCIONAL;
        }
        if (request.legacyKind() == null) {
            return DestinatarioProcessualKind.TERCEIRO;
        }
        return switch (request.legacyKind()) {
            case PESSOA_FISICA -> DestinatarioProcessualKind.PESSOA_FISICA;
            case PESSOA_JURIDICA -> Boolean.TRUE.equals(request.fazendaPublica())
                    ? DestinatarioProcessualKind.ORGAO_INSTITUCIONAL
                    : DestinatarioProcessualKind.PESSOA_JURIDICA;
            case ADVOGADO_OAB -> DestinatarioProcessualKind.ADVOGADO;
            case DEFENSOR_PUBLICO,
                    MINISTERIO_PUBLICO,
                    FAZENDA_PUBLICA,
                    JUIZO_DEPRECADO,
                    ADVOCACIA_PUBLICA,
                    DELEGACIA_POLICIA,
                    POLICIA_PENAL,
                    UNIDADE_PRISIONAL,
                    CONSELHO_TUTELAR,
                    PERITO_JUDICIAL,
                    CONTADORIA_JUDICIAL,
                    EQUIPE_PSICOSSOCIAL,
                    CEJUSC,
                    CARTORIO_EXTRAJUDICIAL,
                    ORGAO_TECNICO_CONVENIADO,
                    ORGAO_JUDICIAL_EXTERNO -> DestinatarioProcessualKind.ORGAO_INSTITUCIONAL;
            default -> throw new IllegalStateException("Legacy recipient kind não suportado: " + request.legacyKind());
        };
    }

    private TrilhoComunicacaoProcessual resolveTrack(DestinatarioProcessualKind processualKind,
                                                     ResolucaoDestinatarioProcessualRequest request,
                                                     DestinatarioInstitucionalKind institucionalKind,
                                                     List<String> justificativas) {
        if (processualKind.isInstitucional() || institucionalKind != null) {
            return TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA;
        }
        if (processualKind == DestinatarioProcessualKind.ADVOGADO) {
            return TrilhoComunicacaoProcessual.REPRESENTACAO_PROCESSUAL;
        }
        if (processualKind == DestinatarioProcessualKind.PESSOA_FISICA && Boolean.TRUE.equals(request.possuiAdvogado()) && request.tipoComunicacao() != null && request.tipoComunicacao().isIntimacao()) {
            justificativas.add("pessoa física intimada com representante digital disponível");
            return TrilhoComunicacaoProcessual.REPRESENTACAO_PROCESSUAL;
        }
        return TrilhoComunicacaoProcessual.PESSOAL_DIRETO;
    }

    private PapelProcessualInstitucional defaultPapel(DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case MINISTERIO_PUBLICO -> PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA;
            case DEFENSORIA_PUBLICA,
                    ADVOCACIA_PUBLICA,
                    PROCURADORIA_ESTADO,
                    PROCURADORIA_MUNICIPIO,
                    AGU,
                    FAZENDA_PUBLICA -> PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE;
            case JUIZO_DEPRECADO, ORGAO_JUDICIAL_EXTERNO -> PapelProcessualInstitucional.JUIZO_COOPERANTE;
            case PERICIA_JUDICIAL, PERITO_JUDICIAL, CEJUSC -> PapelProcessualInstitucional.AUXILIAR_JUSTICA;
            case CONTADORIA_JUDICIAL, EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL, ORGAO_TECNICO_CONVENIADO -> PapelProcessualInstitucional.APOIO_TECNICO;
            case DELEGACIA_POLICIA, DELEGACIA_POLICIA_CIVIL, DELEGACIA_POLICIA_FEDERAL, CONSELHO_TUTELAR -> PapelProcessualInstitucional.ORGAO_REQUISITADO;
            case POLICIA_PENAL, UNIDADE_PRISIONAL -> PapelProcessualInstitucional.UNIDADE_EXECUTORA;
            case CARTORIO_EXTRAJUDICIAL -> PapelProcessualInstitucional.DESTINATARIO_OFICIO;
            default -> throw new IllegalStateException("Destinatário institucional sem papel padrão: " + kind);
        };
    }

    private String resolveDisplayName(ResolucaoDestinatarioProcessualRequest request,
                                      DestinatarioProcessualKind processualKind,
                                      DestinatarioInstitucionalKind institucionalKind) {
        if (request.nome() != null && !request.nome().isBlank()) {
            return request.nome().trim();
        }
        if (institucionalKind != null) {
            return switch (processualKind) {
                case UNIDADE_INSTITUCIONAL -> institucionalKind.name() + "::" + trimToNull(request.unidadeInstitucionalCodigo());
                case ORGAO_INSTITUCIONAL -> institucionalKind.name();
                default -> institucionalKind.name();
            };
        }
        return switch (processualKind) {
            case ADVOGADO -> "ADVOGADO_PROCESSUAL";
            case PESSOA_JURIDICA -> "PESSOA_JURIDICA_PROCESSUAL";
            case PESSOA_FISICA -> "PESSOA_FISICA_PROCESSUAL";
            case PARTE -> "PARTE_PROCESSUAL";
            case TERCEIRO -> "TERCEIRO_PROCESSUAL";
            case AUXILIAR_JUSTICA -> "AUXILIAR_JUSTICA";
            case ORGAO_INSTITUCIONAL -> "ORGAO_INSTITUCIONAL";
            case UNIDADE_INSTITUCIONAL -> "UNIDADE_INSTITUCIONAL";
            default -> processualKind.name();
        };
    }

    private String normalizeDocument(String raw,
                                     DestinatarioProcessualKind processualKind,
                                     DestinatarioInstitucionalKind institucionalKind,
                                     String unidadeInstitucionalCodigo) {
        String normalized = trimToNull(raw);
        if (normalized != null) {
            String digits = normalized.replaceAll("\\D", "");
            return digits.isBlank() ? normalized.toUpperCase(Locale.ROOT) : digits;
        }
        if (institucionalKind == null) {
            return null;
        }
        return processualKind == DestinatarioProcessualKind.UNIDADE_INSTITUCIONAL && unidadeInstitucionalCodigo != null && !unidadeInstitucionalCodigo.isBlank()
                ? "INST-UNIDADE-" + unidadeInstitucionalCodigo.trim().toUpperCase(Locale.ROOT)
                : "INST-" + institucionalKind.name();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String upper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }
}
