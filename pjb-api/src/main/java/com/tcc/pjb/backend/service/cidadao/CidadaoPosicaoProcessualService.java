package com.tcc.pjb.backend.service.cidadao;

import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;

@Service
public class CidadaoPosicaoProcessualService {

    private final PoloProcessualRepository poloProcessualRepository;

    public CidadaoPosicaoProcessualService(PoloProcessualRepository poloProcessualRepository) {
        this.poloProcessualRepository = Objects.requireNonNull(poloProcessualRepository);
    }

    public CidadaoPosicaoProcessual resolver(Processo processo, String cpf) {
        if (processo == null || cpf == null || cpf.isBlank()) {
            return new CidadaoPosicaoProcessual(null, null);
        }
        Optional<PoloProcessual> polo = processo.getId() == null
                ? Optional.empty()
                : poloProcessualRepository.findByProcessoIdAndDocumento(processo.getId(), cpf).filter(PoloProcessual::isAtivo);
        TipoPolo tipoPolo = polo.map(PoloProcessual::getTipoPolo).orElseGet(() -> poloPelosCpfsDoProcesso(processo, cpf));
        return new CidadaoPosicaoProcessual(tipoPolo, representacao(processo, tipoPolo, polo.orElse(null)));
    }

    private static TipoPolo poloPelosCpfsDoProcesso(Processo processo, String cpf) {
        if (cpf.equals(processo.getParteAutoraCpf())) {
            return TipoPolo.ATIVO;
        }
        if (cpf.equals(processo.getParteReuCpf())) {
            return TipoPolo.PASSIVO;
        }
        return null;
    }

    private static TipoRepresentanteProcessual representacao(Processo processo, TipoPolo tipoPolo, PoloProcessual polo) {
        if (tipoPolo == TipoPolo.ATIVO) {
            TipoRepresentanteProcessual peloInstrumento = peloInstrumento(
                    InstrumentoRepresentacaoProcessual.fromString(processo.getInstrumentoRepresentacaoResolvido()));
            if (peloInstrumento != null) {
                return peloInstrumento;
            }
        }
        if (polo != null && polo.getOabNumero() != null && !polo.getOabNumero().isBlank()) {
            return TipoRepresentanteProcessual.ADVOGADO_PRIVADO;
        }
        return null;
    }

    private static TipoRepresentanteProcessual peloInstrumento(InstrumentoRepresentacaoProcessual instrumento) {
        if (instrumento == null) {
            return null;
        }
        if (instrumento.isJusPostulandi()) {
            return TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO;
        }
        return switch (instrumento) {
            case MANDATO_AD_JUDICIA, MANDATO_AD_JUDICIA_ET_EXTRA, PROCURACAO_APUD_ACTA -> TipoRepresentanteProcessual.ADVOGADO_PRIVADO;
            case DEFENSORIA_PUBLICA_INSTITUCIONAL, CURADORIA_ESPECIAL -> TipoRepresentanteProcessual.DEFENSOR_PUBLICO;
            case MINISTERIO_PUBLICO_FUNCIONAL -> TipoRepresentanteProcessual.MINISTERIO_PUBLICO;
            default -> null;
        };
    }

    public record CidadaoPosicaoProcessual(TipoPolo polo, TipoRepresentanteProcessual representacao) {
    }
}
