package com.tcc.pjb.backend.core.comunicacao.institucional.canonico.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoComunicacaoMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.PoliticaAtoCanonicoProcessual;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.domain.InstitutionalCanonicalCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

@Service
public class InstitutionalCanonicalCatalogApplicationService {

    private final AtoCanonicoComunicacaoMapper mapper;

    public InstitutionalCanonicalCatalogApplicationService(AtoCanonicoComunicacaoMapper mapper) {
        this.mapper = mapper;
    }

    public List<InstitutionalCanonicalCatalogEntry> list() {
        return Arrays.stream(AtoCanonicoProcessual.values())
                .sorted(Comparator.comparing(Enum::name))
                .map(this::toEntry)
                .toList();
    }

    private InstitutionalCanonicalCatalogEntry toEntry(AtoCanonicoProcessual ato) {
        PoliticaAtoCanonicoProcessual politica = mapper.resolve(ato);
        return new InstitutionalCanonicalCatalogEntry(
                politica.atoCanonico(),
                politica.destinatarioKind(),
                politica.papelProcessual(),
                politica.tipoComunicacao(),
                politica.exigeCienciaPessoal(),
                politica.bloqueiaFluxo(),
                politica.gateCode(),
                canalPrincipal(politica),
                fallbacks(politica),
                politica.fundamentoLegal(),
                politica.justificativasPadrao()
        );
    }

    private CanalComunicacaoInstitucional canalPrincipal(PoliticaAtoCanonicoProcessual politica) {
        TipoComunicacaoJudicial tipo = politica.tipoComunicacao();
        if (tipo.isCitacao()) {
            return CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO;
        }
        if (politica.exigeCienciaPessoal() || politica.papelProcessual().exigeCienciaPessoalPreferencial()) {
            return CanalComunicacaoInstitucional.PJB_INBOX;
        }
        if (tipo == TipoComunicacaoJudicial.COMUNICACAO_COOPERACAO_NACIONAL) {
            return CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL;
        }
        return CanalComunicacaoInstitucional.PJB_INBOX;
    }

    private List<CanalComunicacaoInstitucional> fallbacks(PoliticaAtoCanonicoProcessual politica) {
        List<CanalComunicacaoInstitucional> out = new ArrayList<>();
        CanalComunicacaoInstitucional principal = canalPrincipal(politica);
        out.add(principal);
        if (principal != CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO && politica.destinatarioKind().admiteCanalNacionalPessoal()) {
            out.add(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO);
        }
        if (!politica.exigeCienciaPessoal()) {
            out.add(CanalComunicacaoInstitucional.DJEN);
        }
        if (politica.tipoComunicacao() == TipoComunicacaoJudicial.COMUNICACAO_COOPERACAO_NACIONAL) {
            out.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
        }
        out.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
        return out.stream().distinct().toList();
    }
}
