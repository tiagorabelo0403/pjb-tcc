package com.tcc.pjb.backend.core.procedural;

import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.bool;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.containsAny;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.firstNonBlank;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.isBlank;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.normalize;
import static com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport.text;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralTerritorialAnalysisFactory {

    private final NationalProceduralForumAllocationMessages messages;

    public NationalProceduralTerritorialAnalysisFactory(NationalProceduralForumAllocationMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    public NationalProceduralTerritorialAnchor resolveTerritorialAnchor(Map<String, Object> payload,
                                                                       String corpus,
                                                                       TipoJustica tipoJustica,
                                                                       String actionNature,
                                                                       String actionFamily,
                                                                       String cidadeBase,
                                                                       String ufBase) {
        String cidadeAutor = firstNonBlank(text(payload.get("comarcaAutor")), text(payload.get("cidadeAutor")), text(payload.get("municipioAutor")));
        String ufAutor = firstNonBlank(text(payload.get("ufAutor")), text(payload.get("estadoAutor")));
        String cidadeReu = firstNonBlank(text(payload.get("comarcaReu")), text(payload.get("cidadeReu")), text(payload.get("municipioReu")));
        String ufReu = firstNonBlank(text(payload.get("ufReu")), text(payload.get("estadoReu")));
        String foroExpresso = firstNonBlank(text(payload.get("foro")), text(payload.get("comarca")), text(payload.get("cidade")));
        String ufExpresso = firstNonBlank(text(payload.get("uf")), text(payload.get("estado")));
        String normalizedCorpus = normalize(corpus);
        boolean federalHint = tipoJustica == TipoJustica.FEDERAL
                || containsAny(actionFamily, "PREVIDENCIARIO", "EXECUCAO_FISCAL")
                || containsAny(normalizedCorpus, "UNIAO", "AUTARQUIA FEDERAL", "INSS", "TRF");
        boolean trabalhoHint = tipoJustica == TipoJustica.TRABALHO || containsAny(normalizedCorpus, "CLT", "VINCULO EMPREGATICIO", "VERBAS RESCISORIAS");
        boolean penalHint = containsAny(actionNature, "PENAL") || containsAny(normalizedCorpus, "DENUNCIA", "QUEIXA CRIME", "HOMICID", "CPP", "INQUERITO");
        if (!isBlank(foroExpresso) || !isBlank(ufExpresso)) {
            return new NationalProceduralTerritorialAnchor(
                    "FORO_EXPRESSO",
                    firstNonBlank(foroExpresso, cidadeBase, cidadeAutor, cidadeReu),
                    firstNonBlank(ufExpresso, ufBase, ufAutor, ufReu),
                    messages.territorialExpressForumReason()
            );
        }
        if (federalHint && !isBlank(cidadeAutor) && !isBlank(ufAutor)) {
            return new NationalProceduralTerritorialAnchor(
                    "DOMICILIO_AUTOR_FEDERAL",
                    cidadeAutor,
                    ufAutor,
                    messages.territorialFederalDomicileReason()
            );
        }
        String cidadePrestacaoServico = cidadePrestacaoServico(payload, cidadeAutor, cidadeReu);
        if (trabalhoHint && !isBlank(cidadePrestacaoServico)) {
            return new NationalProceduralTerritorialAnchor(
                    "LOCAL_PRESTACAO_TRABALHO",
                    cidadePrestacaoServico,
                    firstNonBlank(text(payload.get("ufPrestacaoServico")), ufAutor, ufReu, ufBase),
                    messages.territorialLaborReason()
            );
        }
        if (penalHint && !isBlank(cidadeReu)) {
            return new NationalProceduralTerritorialAnchor(
                    "LOCAL_FATO_PENAL",
                    cidadeReu,
                    firstNonBlank(ufReu, ufBase),
                    messages.territorialPenalReason()
            );
        }
        if (!isBlank(cidadeBase) || !isBlank(ufBase)) {
            return new NationalProceduralTerritorialAnchor(
                    "BASE_RESOLVIDA",
                    cidadeBase,
                    ufBase,
                    messages.territorialResolvedBaseReason()
            );
        }
        if (!isBlank(cidadeAutor) || !isBlank(ufAutor)) {
            return new NationalProceduralTerritorialAnchor(
                    "DOMICILIO_AUTOR",
                    cidadeAutor,
                    ufAutor,
                    messages.territorialAuthorFallbackReason()
            );
        }
        if (!isBlank(cidadeReu) || !isBlank(ufReu)) {
            return new NationalProceduralTerritorialAnchor(
                    "DOMICILIO_REU",
                    cidadeReu,
                    ufReu,
                    messages.territorialDefendantFallbackReason()
            );
        }
        return new NationalProceduralTerritorialAnchor(
                "INDEFINIDO",
                null,
                null,
                messages.territorialUndefinedReason()
        );
    }

    public boolean detectSpecializedVara(String tipoVara, String varaSugerida, String varaFamily) {
        String seed = firstNonBlank(tipoVara, varaSugerida, varaFamily);
        return containsAny(seed,
                "FAZENDA",
                "FAMILIA",
                "ORFAOS",
                "SUCESSOES",
                "EMPRESARIAL",
                "FALENCIA",
                "RECUPERACAO",
                "EXECUCOES PENAIS",
                "TRIBUNAL DO JURI",
                "JUIZADO",
                "PREVIDENCIARIA",
                "AUDITORIA MILITAR",
                "ZONA ELEITORAL",
                "TRABALHO");
    }

    private String cidadePrestacaoServico(Map<String, Object> payload, String cidadeAutor, String cidadeReu) {
        return firstNonBlank(
                text(payload.get("comarcaPrestacaoServico")),
                text(payload.get("cidadePrestacaoServico")),
                text(payload.get("localPrestacaoServico")),
                cidadeAutor,
                cidadeReu
        );
    }
}
