package com.tcc.pjb.backend.service.institucional;

import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class InstituicaoResolucaoService {

    private final InstituicaoRepository instituicaoRepository;

    public InstituicaoResolucaoService(InstituicaoRepository instituicaoRepository) {
        this.instituicaoRepository = Objects.requireNonNull(instituicaoRepository);
    }

    public List<Instituicao> resolverPorKind(DestinatarioInstitucionalKind kind) {
        return traduzirKind(kind)
                .map(instituicaoRepository::findByTipo)
                .orElse(List.of());
    }

    private static Optional<TipoInstituicao> traduzirKind(DestinatarioInstitucionalKind kind) {
        if (kind == null) return Optional.empty();
        return switch (kind) {
            case MINISTERIO_PUBLICO -> Optional.of(TipoInstituicao.MINISTERIO_PUBLICO);
            case DEFENSORIA_PUBLICA -> Optional.of(TipoInstituicao.DEFENSORIA_PUBLICA);
            case ADVOCACIA_PUBLICA -> Optional.of(TipoInstituicao.ADVOCACIA_PUBLICA);
            case PROCURADORIA_ESTADO -> Optional.of(TipoInstituicao.PROCURADORIA_ESTADO);
            case PROCURADORIA_MUNICIPIO -> Optional.of(TipoInstituicao.PROCURADORIA_MUNICIPIO);
            case AGU -> Optional.of(TipoInstituicao.AGU);
            case FAZENDA_PUBLICA -> Optional.of(TipoInstituicao.FAZENDA_PUBLICA);
            case DELEGACIA_POLICIA,
                 DELEGACIA_POLICIA_CIVIL,
                 DELEGACIA_POLICIA_FEDERAL -> Optional.of(TipoInstituicao.DELEGACIA_POLICIA);
            case POLICIA_PENAL -> Optional.of(TipoInstituicao.POLICIA_PENAL);
            case UNIDADE_PRISIONAL -> Optional.of(TipoInstituicao.UNIDADE_PRISIONAL);
            case CONSELHO_TUTELAR -> Optional.of(TipoInstituicao.CONSELHO_TUTELAR);
            case PERICIA_JUDICIAL,
                 PERITO_JUDICIAL -> Optional.of(TipoInstituicao.ORGAO_PERICIAL);
            case CEJUSC -> Optional.of(TipoInstituicao.CEJUSC);
            case CARTORIO_EXTRAJUDICIAL -> Optional.of(TipoInstituicao.CARTORIO_EXTRAJUDICIAL);
            case CONTADORIA_JUDICIAL,
                 EQUIPE_PSICOSSOCIAL,
                 ASSISTENTE_SOCIAL_JUDICIAL,
                 ORGAO_TECNICO_CONVENIADO,
                 JUIZO_DEPRECADO,
                 ORGAO_JUDICIAL_EXTERNO -> Optional.empty();
        };
    }
}
