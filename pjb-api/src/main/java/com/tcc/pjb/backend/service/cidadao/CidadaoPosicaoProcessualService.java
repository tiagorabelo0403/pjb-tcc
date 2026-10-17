package com.tcc.pjb.backend.service.cidadao;

import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.access.PartyMatchResult;
import com.tcc.pjb.backend.core.security.access.PartyRole;
import com.tcc.pjb.backend.core.security.access.ProcessoPartyCpfMatcher;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;

@Service
public class CidadaoPosicaoProcessualService {

    private final PoloProcessualRepository poloProcessualRepository;
    private final ProcessoPartyCpfMatcher partyCpfMatcher;

    public CidadaoPosicaoProcessualService(PoloProcessualRepository poloProcessualRepository,
                                           ProcessoPartyCpfMatcher partyCpfMatcher) {
        this.poloProcessualRepository = Objects.requireNonNull(poloProcessualRepository);
        this.partyCpfMatcher = Objects.requireNonNull(partyCpfMatcher);
    }

    public TipoPolo polo(Processo processo, String cpf) {
        PartyMatchResult match = partyCpfMatcher.match(cpf, processo);
        if (match instanceof PartyMatchResult.Matched matched) {
            return matched.role() == PartyRole.REU ? TipoPolo.PASSIVO : TipoPolo.ATIVO;
        }
        return null;
    }

    public CidadaoPosicaoProcessual resolver(Processo processo, String cpf) {
        if (processo == null || cpf == null || cpf.isBlank()) {
            return new CidadaoPosicaoProcessual(null, null);
        }
        Optional<PoloProcessual> linha = processo.getId() == null
                ? Optional.empty()
                : poloProcessualRepository.findFirstByProcessoIdAndDocumentoAndAtivoTrueOrderByIdAsc(processo.getId(), cpf);
        TipoPolo tipoPolo = linha.map(PoloProcessual::getTipoPolo).orElseGet(() -> polo(processo, cpf));
        return new CidadaoPosicaoProcessual(tipoPolo, representacao(processo, cpf, tipoPolo, linha.orElse(null)));
    }

    private static TipoRepresentanteProcessual representacao(Processo processo, String cpf, TipoPolo tipoPolo, PoloProcessual linha) {
        if (tipoPolo == TipoPolo.ATIVO) {
            TipoRepresentanteProcessual peloInstrumento = peloInstrumento(
                    InstrumentoRepresentacaoProcessual.fromString(processo.getInstrumentoRepresentacaoResolvido()));
            if (peloInstrumento != null) {
                return peloInstrumento;
            }
        }
        if (linha != null && linha.isAdvogado()) {
            return TipoRepresentanteProcessual.ADVOGADO_PRIVADO;
        }
        return tipoPolo == TipoPolo.ATIVO ? peloAtorDoAjuizamento(processo.getUsuario(), cpf) : null;
    }

    private static TipoRepresentanteProcessual peloAtorDoAjuizamento(Usuario ator, String cpf) {
        TipoUsuario tipo = ator == null ? null : ator.getTipoUsuario();
        if (tipo == null) {
            return null;
        }
        if (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isMinisterioPublico()) {
            return TipoRepresentanteProcessual.doAtor(tipo);
        }
        return cpf.equals(ator.getCpf()) ? TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO : null;
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
